import {
	API_BASE,
	decryptFileMeta,
	getFileCryptoContext,
	getFileCryptoContextForMetas,
	decryptFileStream,
	encryptFileMeta,
	unwrapKey,
	decryptPolicyKeyForRecipient,
	decryptMetaKeyWithPolicyKey,
	decryptFileKeyWithPolicyKey,
	type FileMetaCrypto,
	type FileMetaSummaryPage,
	type FileMetaFullPage,
	type FileMetaFull,
	type FileMetaWithId,
	type FileMeta,
	type FileMetaAccess,
	type RecipientUnlockRecord,
	type FileContentAccess,
	type DownloadableZipFile,
	encryptFileStream,
	type FileStorage,
	keyedContentHashB64,
	preserveOldExtension,
	type StorageType,
	verifyPolicyManifest
} from '$lib';
import sodium from 'libsodium-wrappers-sumo';
import JSZip from 'jszip';

async function apiGet<T>(
	path: string,
	errorMsg: string,
	purpose: 'file' | 'meta',
	params?: URLSearchParams
): Promise<T> {
	const url = `${API_BASE}${path}${params ? '?' + params.toString() : ''}`;
	const res = await fetch(url, {
		credentials: 'include'
	});
	if (!res.ok) throw new Error(`${errorMsg} (status ${res.status})`);
	if (purpose === 'meta') return res.json() as T;
	return res.blob() as T;
}

export type PromptDeps = {
	askSecretPhrase: () => Promise<string>;
	askNewFileName?: () => Promise<string>;
};

export async function fetchFilesMetaSummary(
	page: number,
	size: number
): Promise<FileMetaSummaryPage> {
	return apiGet<FileMetaSummaryPage>(
		'/file-storage/meta-sum',
		'Nepavyko gauti metaduomenų informacijos.',
		'meta',
		new URLSearchParams({
			page: String(page),
			size: String(size)
		})
	);
}

export async function fetchFileMetaFull(fileId: string): Promise<FileMetaFull> {
	return apiGet<FileMetaFull>(
		`/file-storage/${fileId}/meta`,
		'Nepavyko gauti failo metaduomenų!',
		'meta'
	);
}

export async function fetchFilesMetas(page: number, size: number): Promise<FileMetaFullPage> {
	return apiGet<FileMetaFullPage>(
		`/file-storage/meta`,
		'Nepavyko gauti failų metaduomenų!',
		'meta',
		new URLSearchParams({
			page: String(page),
			size: String(size)
		})
	);
}

export async function fetchFileMetaCrypto(fileId: string): Promise<FileMetaCrypto> {
	return apiGet<FileMetaCrypto>(
		`/file-storage/${fileId}/meta-crypto`,
		'Nepavyko gauti failo metaduomenų šifravimo informacijos!',
		'meta'
	);
}

export async function fetchEncryptedFileBlob(fileId: string) {
	return apiGet<Blob>(
		`/file-storage/${fileId}/download`,
		'Nepavyko gauti užšifruoto failo!',
		'file'
	);
}

export async function fetchReceivedFileBlob(policyId: string, fileId: string) {
	return apiGet<Blob>(
		`/received-policies/${policyId}/file/${fileId}/download`,
		'Nepavyko gauti gauto failo!',
		'file'
	);
}

export async function decryptMetaForRow(
	fileId: string,
	purpose: 'full',
	secretPhrase?: string,
	deps?: PromptDeps
): Promise<{ metaFromApi: FileMetaFull; fileMeta: FileMeta }>;

export async function decryptMetaForRow(
	fileId: string,
	purpose: 'crypto',
	secretPhrase?: string,
	deps?: PromptDeps
): Promise<{
	userId: string;
	metaKey: Uint8Array;
	metaFromApi: FileMetaCrypto;
	fileMeta: FileMeta;
}>;

export async function decryptMetaForRow(
	fileId: string,
	purpose: 'full' | 'crypto',
	secretPhrase?: string,
	deps?: PromptDeps
): Promise<
	| { metaFromApi: FileMetaFull; fileMeta: FileMeta }
	| {
			userId: string;
			metaKey: Uint8Array;
			metaFromApi: FileMetaCrypto;
			fileMeta: FileMeta;
	  }
> {
	const metaFromApi =
		purpose === 'full' ? await fetchFileMetaFull(fileId) : await fetchFileMetaCrypto(fileId);
	const phrase = secretPhrase ?? (deps ? await deps?.askSecretPhrase() : undefined);
	if (!phrase) throw new Error('Įveskite šifravimo slaptažodį');

	const { userId, metaKey } = await getFileCryptoContext(
		phrase,
		'meta',
		BigInt(metaFromApi.subKeyId)
	);
	// const shouldWipeHere = purpose === 'full';

	try {
		const fileMeta = await decryptFileMeta(
			{ metaNonceB64: metaFromApi.metaNonceB64, metaCipherB64: metaFromApi.metaCipherB64 },
			metaKey,
			{ userId, fileId, vaultVersion: 1 }
		);
		return purpose === 'full'
			? { metaFromApi: metaFromApi as FileMetaFull, fileMeta }
			: { userId, metaKey, metaFromApi, fileMeta };
	} finally {
		// if (shouldWipeHere) sodium.memzero(metaKey);
	}
}

type MetaKey = {
	userId: string;
	subKeyId: bigint;
	metaKey: Uint8Array;
};

export async function decryptAllMeta(
	page: number,
	size: number,
	deps: PromptDeps
): Promise<{ metasFromApi: FileMetaFullPage; fileMetaWithId: FileMetaWithId[] }> {
	const metasFromApi = await fetchFilesMetas(page, size);
	const secretPhrase = await deps.askSecretPhrase();
	let metaKeys: MetaKey[] = [];
	let keyBySubkeyId: Map<bigint, MetaKey> | null = null;

	try {
		metaKeys = await getFileCryptoContextForMetas(
			secretPhrase,
			metasFromApi.fileMetaFull.map((m) => BigInt(m.subKeyId))
		);

		keyBySubkeyId = new Map<bigint, MetaKey>(metaKeys.map((m) => [m.subKeyId, m]));
		const fileMetaWithId = await Promise.all(
			metasFromApi.fileMetaFull.map(async (m) => {
				const subKeyId = BigInt(m.subKeyId);
				const mk = keyBySubkeyId!.get(subKeyId);
				if (!mk) throw new Error(`Meta key not found for subKeyId ${subKeyId}`);

				const fileMeta = await decryptFileMeta(
					{
						metaNonceB64: m.metaNonceB64,
						metaCipherB64: m.metaCipherB64
					},
					mk.metaKey,
					{
						userId: mk.userId,
						fileId: m.id,
						vaultVersion: 1
					}
				);
				return { id: m.id, ...fileMeta };
			})
		);
		return { metasFromApi, fileMetaWithId };
	} finally {
		for (const k of metaKeys) {
			if (k.metaKey) sodium.memzero(k.metaKey);
		}
		metaKeys.length = 0;
		keyBySubkeyId?.clear();
		keyBySubkeyId = null;
	}
}

export async function decryptFileBlob(
	fileId: string,
	blob: Blob,
	deps: PromptDeps
): Promise<{ blob: Blob; fileMeta: FileMeta }> {
	const secretPhrase = await deps.askSecretPhrase();
	const { metaFromApi, fileMeta } = await decryptMetaForRow(fileId, 'crypto', secretPhrase);
	const { userId, fileKey } = await getFileCryptoContext(
		secretPhrase,
		'file',
		BigInt(metaFromApi.subKeyId)
	);
	try {
		return { blob: await decryptFileStream(blob, fileKey, userId, fileId, 1), fileMeta };
	} finally {
		if (fileKey) sodium.memzero(fileKey);
	}
}

export function saveBlob(blob: Blob, fileName: string): void {
	const url = URL.createObjectURL(blob);
	try {
		const a = document.createElement('a');
		a.href = url;
		a.download = fileName;
		document.body.appendChild(a);
		a.click();
		a.remove();
	} finally {
		URL.revokeObjectURL(url);
	}
}

export async function saveFilesAsZip(
	files: DownloadableZipFile[],
	zipName = 'files.zip'
): Promise<void> {
	if (!files.length) {
		throw new Error('Nėra failų archyvavimui.');
	}

	// Creates empty ZIP
	const zip = new JSZip();

	for (const file of files) {
		// Adds file to ZIP
		zip.file(file.fileName, file.blob);
	}

	// Generating the final ZIP file in memory as a Blob.
	const blob = await zip.generateAsync({ type: 'blob' });
	saveBlob(blob, zipName);
}

export async function saveFileName(
	userId: string,
	metaKey: Uint8Array,
	fileId: string,
	fileName: string,
	fileMeta: FileMeta
): Promise<string> {
	const finalFileName = preserveOldExtension(fileMeta.fileName, fileName);

	const updatedFileMeta: FileMeta = {
		...fileMeta,
		fileName: finalFileName
	};

	const { metaNonceB64, metaCipherB64 } = await encryptFileMeta(updatedFileMeta, metaKey, {
		userId,
		fileId,
		vaultVersion: 1
	});

	const res = await fetch(`${API_BASE}/file-storage/${fileId}/rename`, {
		method: 'PATCH',
		credentials: 'include',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify({ metaNonceB64, metaCipherB64 })
	});

	if (!res.ok) throw new Error(`Nepavyko atnaujinti failo pavadinimo: ${res.status}`);

	return finalFileName;
}

export async function fileDelete(fileId: string): Promise<void> {
	const res = await fetch(`${API_BASE}/file-storage/${fileId}`, {
		method: 'DELETE',
		credentials: 'include'
	});
	if (!res.ok) {
		throw new Error(`Nepavyko ištrinti failo: ${res.status}`);
	}
}

export async function decryptReceivedFileMetaForRow(
	policyId: string,
	fileId: string,
	deps: PromptDeps,
	secretPhrase_?: string,
	purpose: 'VIEW' | 'DOWNLOAD' = 'VIEW'
): Promise<FileMeta> {
	const fileMetaAccess = await apiGet<FileMetaAccess>(
		`/received-policies/${policyId}/file-meta/${fileId}`,
		'Nepavyko gauti failo metaduomenų prieigos duomenų!',
		'meta',
		new URLSearchParams({ purpose })
	);

	const recipientUnlockKeys = await apiGet<RecipientUnlockRecord>(
		`/vault/recipient-unlock`,
		'Nepavyko gauti naudotojo raktų!',
		'meta'
	);

	const secretPhrase = secretPhrase_ ?? (await deps.askSecretPhrase());

	const recipientPrivateKey = await unwrapKey(
		secretPhrase,
		recipientUnlockKeys.userId,
		recipientUnlockKeys
	);

	const isManifestValid = await verifyPolicyManifest(
		fileMetaAccess.manifest,
		fileMetaAccess.signatureByOwnerB64,
		fileMetaAccess.ownerSignPublicKeyB64
	);

	if (!isManifestValid) {
		throw new Error('Politikos manifestas arba savininko parašas neteisingas!');
	}

	const policyKey = await decryptPolicyKeyForRecipient(
		fileMetaAccess.encryptedPolicyKeyForRecipientB64,
		recipientPrivateKey,
		recipientUnlockKeys.recipientPublicKeyB64
	);

	const metaKey = await decryptMetaKeyWithPolicyKey(
		fileMetaAccess.encryptedMetaKeyByPolicyB64,
		fileMetaAccess.encryptedMetaKeyNonceB64,
		policyKey
	);

	const fileMeta = await decryptFileMeta(
		{
			metaNonceB64: fileMetaAccess.metaNonceB64,
			metaCipherB64: fileMetaAccess.metaCipherB64
		},
		metaKey,
		{
			userId: fileMetaAccess.ownerUserId,
			fileId: fileMetaAccess.fileId,
			vaultVersion: recipientUnlockKeys.vaultVersion
		}
	);

	return fileMeta;
}

export async function decryptReceivedFileBlob(
	policyId: string,
	fileId: string,
	blob: Blob,
	deps: PromptDeps,
	secretPhrase_?: string
) {
	const fileContentAccess = await apiGet<FileContentAccess>(
		`/received-policies/${policyId}/file-content/${fileId}`,
		'Nepavyko gauti failo turinio prieigos duomenų!',
		'meta'
	);

	const recipientUnlockKeys = await apiGet<RecipientUnlockRecord>(
		`/vault/recipient-unlock`,
		'Nepavyko gauti naudotojo raktų!',
		'meta'
	);

	const secretPhrase = secretPhrase_ ?? (await deps.askSecretPhrase());

	const recipientPrivateKey = await unwrapKey(
		secretPhrase,
		recipientUnlockKeys.userId,
		recipientUnlockKeys
	);

	const isManifestValid = await verifyPolicyManifest(
		fileContentAccess.manifest,
		fileContentAccess.signatureByOwnerB64,
		fileContentAccess.ownerSignPublicKeyB64
	);

	if (!isManifestValid) {
		throw new Error('Politikos manifestas arba savininko parašas neteisingas!');
	}

	const policyKey = await decryptPolicyKeyForRecipient(
		fileContentAccess.encryptedPolicyKeyForRecipientB64,
		recipientPrivateKey,
		recipientUnlockKeys.recipientPublicKeyB64
	);

	const fileKey = await decryptFileKeyWithPolicyKey(
		fileContentAccess.encryptedFileKeyByPolicyB64,
		fileContentAccess.encryptedFileKeyNonceB64,
		policyKey
	);

	try {
		return {
			blob: await decryptFileStream(
				blob,
				fileKey,
				fileContentAccess.ownerUserId,
				fileId,
				recipientUnlockKeys.vaultVersion
			),
			fileMeta: await decryptReceivedFileMetaForRow(
				policyId,
				fileId,
				deps,
				secretPhrase,
				'DOWNLOAD'
			)
		};
	} finally {
		if (fileKey) sodium.memzero(fileKey);
	}
}

export async function uploadOneFile(
	file: File,
	secretPhrase: string,
	storageType: StorageType
): Promise<void> {
	let fileKey: Uint8Array | null = null;
	let metaKey: Uint8Array | null = null;
	let hashKey: Uint8Array | null = null;

	try {
		const rawMeta: FileMeta = {
			fileName: file.name,
			size: file.size,
			mimeType: file.type,
			lastModified: file.lastModified,
			relativePath: file.webkitRelativePath ?? ''
		};

		const cryptoContext = await getFileCryptoContext(secretPhrase, 'file');

		const userId = cryptoContext.userId;
		const subKeyId = cryptoContext.subKeyId;
		fileKey = cryptoContext.fileKey;
		metaKey = cryptoContext.metaKey;
		hashKey = cryptoContext.hashKey;

		const contentHash = await keyedContentHashB64(file, hashKey);

		const { fileId, cipherBlob } = await encryptFileStream(fileKey, file, userId, 1);

		const { metaNonceB64, metaCipherB64 } = await encryptFileMeta(rawMeta, metaKey, {
			userId,
			fileId,
			vaultVersion: 1
		});

		const form = new FormData();

		const meta: FileStorage = {
			fileId,
			subKeyId,
			contentHashB64: contentHash,
			metaNonceB64,
			metaCipherB64,
			storageType
		};

		form.append(
			'meta',
			new Blob([JSON.stringify(meta, (_, v) => (typeof v === 'bigint' ? v.toString() : v))], {
				type: 'application/json'
			})
		);

		form.append('blob', new Blob([cipherBlob], { type: 'application/octet-stream' }));

		const uploadRes = await fetch(`${API_BASE}/file-storage/upload`, {
			method: 'POST',
			credentials: 'include',
			body: form
		});

		if (!uploadRes.ok) {
			throw new Error('Failas jau egzistuoja arba jo įkelti nepavyko!');
		}
	} finally {
		if (fileKey) sodium.memzero(fileKey);
		if (metaKey) sodium.memzero(metaKey);
		if (hashKey) sodium.memzero(hashKey);
	}
}
