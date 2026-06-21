import sodium from 'libsodium-wrappers-sumo';
import { b64, fromb64 } from './encoding';

export type FileKey = {
	fileId: string;
	fileKey: Uint8Array;
	metaKey: Uint8Array;
};

export type FileSubKeyId = {
	fileId: string;
	subKeyId: string;
};

export type RecipientPublicKey = {
	recipientUserId: string;
	publicKeyB64: string;
};

export type EncryptedFileKey = {
	fileId: string;
	fileKeyEncryptedB64: string;
	fileKeyNonceB64: string;
	metaKeyEncryptedB64: string;
	metaKeyNonceB64: string;
};

export type EncryptedRecipientPolicyKey = {
	recipientUserId: string;
	encryptedPolicyKeyForRecipientB64: string;
};

export type Manifest = {
	fileIds: string[];
	policyId: string;
	recipientUserId: string;
	encryptedPolicyKeyForRecipientB64: string;
	createdAt: string;
};

export async function generatePolicyKey(): Promise<Uint8Array> {
	await sodium.ready;
	return sodium.randombytes_buf(32);
}

export async function encryptFileKeysWithPolicyKey(
	fileKeys: FileKey[],
	policyKey: Uint8Array
): Promise<EncryptedFileKey[]> {
	await sodium.ready;

	return fileKeys.map(({ fileId, fileKey, metaKey }) => {
		const fileKeyNonce = sodium.randombytes_buf(
			sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
		);

		const metaKeyNonce = sodium.randombytes_buf(
			sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
		);

		const fileKeyEncrypted = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
			fileKey,
			null,
			null,
			fileKeyNonce,
			policyKey
		);

		const metaKeyEncrypted = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
			metaKey,
			null,
			null,
			metaKeyNonce,
			policyKey
		);

		return {
			fileId,
			fileKeyEncryptedB64: b64(fileKeyEncrypted),
			fileKeyNonceB64: b64(fileKeyNonce),
			metaKeyEncryptedB64: b64(metaKeyEncrypted),
			metaKeyNonceB64: b64(metaKeyNonce)
		};
	});
}

export async function encryptPolicyKeyForRecipients(
	policyKey: Uint8Array,
	recipientsPublicKeys: RecipientPublicKey[]
): Promise<EncryptedRecipientPolicyKey[]> {
	await sodium.ready;

	return recipientsPublicKeys.map(({ recipientUserId, publicKeyB64 }) => ({
		recipientUserId,
		encryptedPolicyKeyForRecipientB64: b64(sodium.crypto_box_seal(policyKey, fromb64(publicKeyB64)))
	}));
}

export async function signPolicyManifest(
	fileIds: string[],
	encryptedRecipientPolicyKeys: EncryptedRecipientPolicyKey[],
	ownerSignPrivateKey: Uint8Array,
	policyId?: string
) {
	await sodium.ready;

	policyId = policyId ?? crypto.randomUUID();
	const createdAt = new Date().toISOString();

	const recipientAccesses = encryptedRecipientPolicyKeys.map(
		({ recipientUserId, encryptedPolicyKeyForRecipientB64 }) => {
			const manifest: Manifest = {
				fileIds,
				policyId,
				recipientUserId,
				encryptedPolicyKeyForRecipientB64,
				createdAt
			};

			const signature = sodium.crypto_sign_detached(
				sodium.from_string(JSON.stringify(manifest)),
				ownerSignPrivateKey
			);

			return {
				recipientUserId,
				encryptedPolicyKeyForRecipientB64,
				manifest,
				signatureByOwnerB64: b64(signature)
			};
		}
	);

	return {
		policyId,
		recipientAccesses,
		createdAt
	};
}

export async function verifyPolicyManifest(
	manifest: Manifest,
	signatureByOwnerB64: string,
	ownerSignPublicKeyB64: string
): Promise<boolean> {
	await sodium.ready;

	const manifestBytes = sodium.from_string(JSON.stringify(manifest));
	const signature = fromb64(signatureByOwnerB64);
	const ownerSignPublicKey = fromb64(ownerSignPublicKeyB64);

	return sodium.crypto_sign_verify_detached(signature, manifestBytes, ownerSignPublicKey);
}
