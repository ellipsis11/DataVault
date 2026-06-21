import { type FileMeta, type Manifest } from '$lib';

type FileMetaSummary = {
	id: string;
	storageType: StorageType;
	createdAt: string;
};

export type FileMetaSummaryPage = {
	fileMetaSummary: FileMetaSummary[];
	totalElements: number;
};

export type FileMetaFullPage = {
	fileMetaFull: FileMetaFull[];
	totalElements: number;
};

export type FileRow = {
	id: string;
	storageType: StorageType;
	createdAt: string;
	fileMeta?: FileMeta;
	checked?: boolean;
	loading?: boolean;
};

export type FileMetaFull = {
	id: string;
	storageType: StorageType; ///
	storageRef: string;
	subKeyId: string;
	metaNonceB64: string;
	metaCipherB64: string;
};

export type FileMetaWithId = { id: string } & FileMeta;
export type FileMetaCrypto = Pick<FileMetaFull, 'subKeyId' | 'metaNonceB64' | 'metaCipherB64'>;

export type FileMetaAccess = {
	policyId: string;
	fileId: string;
	ownerUserId: string;
	encryptedPolicyKeyForRecipientB64: string;
	encryptedMetaKeyByPolicyB64: string;
	encryptedMetaKeyNonceB64: string;
	metaCipherB64: string;
	metaNonceB64: string;
	manifest: Manifest;
	signatureByOwnerB64: string;
	ownerSignPublicKeyB64: string;
};

export type FileContentAccess = {
	policyId: string;
	fileId: string;
	ownerUserId: string;
	encryptedPolicyKeyForRecipientB64: string;
	encryptedFileKeyByPolicyB64: string;
	encryptedFileKeyNonceB64: string;
	manifest: Manifest;
	signatureByOwnerB64: string;
	ownerSignPublicKeyB64: string;
};

export type DownloadableZipFile = {
	fileName: string;
	blob: Blob;
};

export type StorageType = 'AWS_S3' | 'FILEBASE_IPFS';

export type FileStorage = {
	fileId: string;
	subKeyId: bigint;
	contentHashB64: string;
	metaNonceB64: string;
	metaCipherB64: string;
	storageType: StorageType;
};
