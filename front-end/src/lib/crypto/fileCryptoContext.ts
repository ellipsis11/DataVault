import { API_BASE } from '$lib/config';
import { getMe, unwrapKey, deriveKeyForFile } from '$lib';

type Purpose = 'file' | 'meta' | 'policy';

/**
 * Loads the current user's vault record, unwraps the vault master key using `secretPhrase`,
 * then derives and returns key material based on `purpose`.
 *
 * - `purpose: 'file'` → returns `userId` + `subKeyId` + `{ fileKey, metaKey, hashKey }`
 * - `purpose: 'meta'` → returns `{ subKeyId, metaKey }` only (for metadata decrypt)
 *
 * @param secretPhrase User secret phrase used to unwrap the master key.
 * @param purpose Which key set to derive: `'file'` or `'meta'`.
 * @returns Derived key material for the requested purpose.
 * @throws Error If vault fetch fails, userId is missing, or unwrap/derivation fails.
 */
export async function getFileCryptoContext(
	secretPhrase: string,
	purpose: 'file',
	subKeyId_?: bigint
): Promise<{
	userId: string;
	subKeyId: bigint;
	fileKey: Uint8Array;
	metaKey: Uint8Array;
	hashKey: Uint8Array;
}>;

export async function getFileCryptoContext(
	secretPhrase: string,
	purpose: 'meta',
	subKeyId_: bigint
): Promise<{ userId: string; subKeyId: bigint; metaKey: Uint8Array }>;

export async function getFileCryptoContext(
	secretPhrase: string,
	purpose: 'policy',
	subKeyId_: bigint
): Promise<{ fileKey: Uint8Array; metaKey: Uint8Array }>;

export async function getFileCryptoContext(
	secretPhrase: string,
	purpose: Purpose,
	subKeyId_?: bigint
) {
	let vaultRes: Response;

	if (purpose === 'policy') {
		vaultRes = await fetch(`${API_BASE}/vault`, {
			credentials: 'include'
		});
	} else {
		vaultRes = await fetch(`${API_BASE}/vault/basic`, {
			credentials: 'include'
		});
	}

	if (!vaultRes.ok) throw new Error('Nepavyko gauti dabartinio naudotojo raktų iš DB!');
	const vaultRecord = await vaultRes.json();

	const userId = (await getMe()).userId;
	if (!userId) throw new Error('Nepavyko gauti dabartinio naudotojo ID iš DB!');

	const masterKey = await unwrapKey(secretPhrase, userId, vaultRecord);

	if (purpose === 'file') {
		const { subKeyId, fileKey, metaKey, hashKey } = await deriveKeyForFile(
			masterKey,
			'file',
			subKeyId_
		);
		return { userId, subKeyId, fileKey, metaKey, hashKey };
	} else if (purpose === 'policy') {
		const { fileKey, metaKey } = await deriveKeyForFile(masterKey, 'file', subKeyId_);
		return { fileKey, metaKey };
	} else {
		if (subKeyId_ === undefined) throw new Error('SubKeyId yra būtinas metaduomenų iššifravimui!');
		return { userId, ...(await deriveKeyForFile(masterKey, 'meta', subKeyId_)) };
	}
}

export async function getFileCryptoContextForMetas(
	secretPhrase: string,
	subKeyIds: bigint[]
): Promise<{ userId: string; subKeyId: bigint; metaKey: Uint8Array }[]> {
	return Promise.all(subKeyIds.map((s) => getFileCryptoContext(secretPhrase, 'meta', s)));
}
