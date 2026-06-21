import { API_BASE } from '$lib/config';
import { getFileCryptoContext } from '$lib/crypto/fileCryptoContext';
import {
	type UserSearchItem,
	type FileSearchItem,
	type DialogSelections,
	type PolicyPage,
	type PolicyDetails,
	type PolicyFormMode,
	type ReceivedPolicyPage,
	type ReceivedPolicyDetails,
	type RecipientValidationItem,
	type PolicyStatus
} from './policyTypes';
import {
	generatePolicyKey,
	encryptFileKeysWithPolicyKey,
	encryptPolicyKeyForRecipients,
	signPolicyManifest,
	unwrapKey,
	apiGet,
	apiPost,
	type FileKey,
	type FileSubKeyId,
	type RecipientPublicKey,
	type VaultRecordBasicForSigning,
	type PromptDeps
} from '$lib';
import sodium from 'libsodium-wrappers-sumo';

export async function searchUsersByEmail(q: string): Promise<UserSearchItem[]> {
	if (!q.trim()) {
		return [];
	}
	return await apiGet<UserSearchItem[]>(
		'/users/search',
		'Įvyko klaida ieškant naudotojų!',
		new URLSearchParams({ query: q })
	);
}

export async function filterFilesByDate(args: {
	fileFrom?: string;
	fileTo?: string;
}): Promise<FileSearchItem[]> {
	const res = await apiGet<FileSearchItem[]>(
		'/file-storage/filter',
		'Įvyko klaida filtruojant failus!',
		new URLSearchParams({ from: args.fileFrom ?? '', to: args.fileTo ?? '' })
	);
	if (!res) throw new Error('Failų filtravimo metu serveris negrąžino atsakymo!');
	return res;
}

export async function submitPolicy(
	dialogSelections: DialogSelections,
	deps: PromptDeps,
	purpose: PolicyFormMode,
	policyId_?: string
): Promise<void> {
	// Awaited gets the real value type (after await)
	let fileKeys: Awaited<ReturnType<typeof getFilesKeys>> = [];
	let policyKey: Uint8Array | null = null;
	let ownerSignPrivateKey: Uint8Array | null = null;

	try {
		const secretPhrase = await deps.askSecretPhrase();

		if (!secretPhrase?.trim()) {
			throw new Error('Šifravimo slaptažodis nebuvo pateiktas!');
		}

		fileKeys = await getFilesKeys(secretPhrase, dialogSelections.files);
		policyKey = await generatePolicyKey();
		const encryptedFileKeys = await encryptFileKeysWithPolicyKey(fileKeys, policyKey);
		const recipientsPublicKeys = await getRecipientPublicKeys(dialogSelections.recipients);
		const encryptedPolicyKey = await encryptPolicyKeyForRecipients(policyKey, recipientsPublicKeys);
		ownerSignPrivateKey = await getOwnerSignPrivateKey(secretPhrase);
		const { policyId, recipientAccesses, createdAt } = await signPolicyManifest(
			dialogSelections.files,
			encryptedPolicyKey,
			ownerSignPrivateKey,
			policyId_
		);

		dialogSelections.policyId = policyId;
		dialogSelections.createdAt = createdAt;

		if (purpose === 'create') {
			await apiPost<void>('/conditional-release/create', 'Nepavyko sukurti politikos!', {
				policy: dialogSelections,
				encryptedFileKeys,
				recipientAccesses
			});
		} else {
			const res = await fetch(`${API_BASE}/conditional-release/policy/${policyId_}`, {
				method: 'PUT',
				credentials: 'include',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					policy: dialogSelections,
					encryptedFileKeys,
					recipientAccesses
				})
			});

			if (!res.ok) {
				throw new Error(`Nepavyko atnaujinti politikos: ${res.status}`);
			}
		}
	} catch (e) {
		throw e;
	} finally {
		fileKeys.forEach((f) => {
			// sodium.memzero(f.fileKey);
			// sodium.memzero(f.metaKey);
		});

		if (policyKey) sodium.memzero(policyKey);
		if (ownerSignPrivateKey) sodium.memzero(ownerSignPrivateKey);
	}
}

async function getFilesKeys(secretPhrase: string, files: string[]): Promise<FileKey[]> {
	let fileKeys: FileKey[] = [];
	const subKeyIds = await apiPost<FileSubKeyId[]>(
		'/file-storage/subKey-ids',
		'Nepavyko gauti subraktų ID!',
		files
	);

	for (const { fileId, subKeyId } of subKeyIds) {
		const { fileKey, metaKey } = await getFileCryptoContext(
			secretPhrase,
			'policy',
			BigInt(subKeyId)
		);
		fileKeys.push({ fileId, fileKey, metaKey });
	}
	return fileKeys;
}

async function getRecipientPublicKeys(recipients: string[]): Promise<RecipientPublicKey[]> {
	return apiPost<RecipientPublicKey[]>(
		'/vault/recipient-public-keys',
		'Nepavyko gauti gavėjų viešųjų raktų!',
		recipients
	);
}

async function getOwnerSignPrivateKey(secretPhrase: string): Promise<Uint8Array> {
	const vaultRes = await apiGet<VaultRecordBasicForSigning>(
		'/vault/owner-sign-private-key',
		'Nepavyko gauti savininko pasirašymo rakto.'
	);

	return unwrapKey(secretPhrase, vaultRes.userId, vaultRes);
}

export async function getPolicyPage(params: {
	query: string;
	filter: PolicyStatus | null;
	page: number;
	size: 10 | 20 | 50 | 100;
}): Promise<PolicyPage> {
	const paramsToSend = new URLSearchParams();

	paramsToSend.set('query', String(params.query));
	paramsToSend.set('page', String(params.page));
	paramsToSend.set('size', String(params.size));

	if (params.filter) {
		paramsToSend.set('filter', params.filter);
	}

	return apiGet<PolicyPage>(
		'/conditional-release/policies',
		'Nepavyko gauti naudotojo politikų puslapio!',
		paramsToSend
	);
}

export async function getPolicyDetails(policyId: string): Promise<PolicyDetails> {
	return apiGet<PolicyDetails>(
		`/conditional-release/policy/${policyId}`,
		'Nepavyko gauti naudotojo politikos!'
	);
}

export async function getReceivedPolicyPage(params: {
	query: string;
	filter: 'NEW' | 'VIEWED' | null;
	page: number;
	size: 10 | 20 | 50 | 100;
}): Promise<ReceivedPolicyPage> {
	const paramsToSend = new URLSearchParams();

	paramsToSend.set('query', String(params.query));
	paramsToSend.set('page', String(params.page));
	paramsToSend.set('size', String(params.size));

	if (params.filter) {
		paramsToSend.set('filter', params.filter);
	}

	return apiGet<ReceivedPolicyPage>(
		'/received-policies',
		'Nepavyko gauti atkleistų politikų puslapio!',
		paramsToSend
	);
}

export async function getReceivedPolicyDetails(policyId: string): Promise<ReceivedPolicyDetails> {
	return apiGet<ReceivedPolicyDetails>(
		`/received-policies/${policyId}`,
		'Nepavyko gauti atskleistos politikos informacijos!'
	);
}

export async function deletePolicy(policyId: string): Promise<void> {
	const res = await fetch(`${API_BASE}/conditional-release/policy/${policyId}`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) {
		throw new Error(`Nepavyko ištrinti politikos: ${res.status}`);
	}
}

export async function releasePolicyNow(policyId: string): Promise<void> {
	return apiPost<void>(
		`/conditional-release/policy/${policyId}/release`,
		'Nepavyko rankiniu būdu atskleisti politikos!',
		{}
	);
}

export async function cancelPolicy(policyId: string): Promise<void> {}

export async function validatePolicyRecipients(
	recipientsIds: string[]
): Promise<RecipientValidationItem[]> {
	return apiPost<RecipientValidationItem[]>(
		'/conditional-release/validate-recipients',
		'Nepavyko patikrinti politikos gavėjų!',
		recipientsIds
	);
}

export async function pausePolicy(policyId: string): Promise<void> {
	return apiPost<void>(
		`/conditional-release/policy/${policyId}/pause`,
		'Nepavyko pristabdyti politikos!',
		{}
	);
}

export async function resumePolicy(policyId: string): Promise<void> {
	return apiPost<void>(
		`/conditional-release/policy/${policyId}/resume`,
		'Politikos tęsti nepavyko!',
		{}
	);
}

export async function deleteReceivedPolicyAccess(policyId: string): Promise<void> {
	const res = await fetch(`${API_BASE}/received-policies/${policyId}/delete`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) {
		throw new Error(`Nepavyko ištrinti gautos politikos: ${res.status}`);
	}
}
