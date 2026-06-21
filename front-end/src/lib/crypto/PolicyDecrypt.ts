import sodium from 'libsodium-wrappers-sumo';
import { fromb64 } from './encoding';

export async function decryptPolicyKeyForRecipient(
	encryptedPolicyKeyForRecipientB64: string,
	recipientPrivateKey: Uint8Array,
	recipientPublicKeyB64: string
): Promise<Uint8Array> {
	await sodium.ready;
	return sodium.crypto_box_seal_open(
		fromb64(encryptedPolicyKeyForRecipientB64),
		fromb64(recipientPublicKeyB64),
		recipientPrivateKey
	);
}

export async function decryptMetaKeyWithPolicyKey(
	encryptedMetaKeyByPolicyB64: string,
	encryptedMetaKeyNonceB64: string,
	policyKey: Uint8Array
): Promise<Uint8Array> {
	await sodium.ready;
	return sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
		null,
		fromb64(encryptedMetaKeyByPolicyB64),
		null,
		fromb64(encryptedMetaKeyNonceB64),
		policyKey
	);
}

export async function decryptFileKeyWithPolicyKey(
	encryptedFileKeyByPolicyB64: string,
	encryptedFileKeyNonceB64: string,
	policyKey: Uint8Array
): Promise<Uint8Array> {
	await sodium.ready;
	return sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
		null,
		fromb64(encryptedFileKeyByPolicyB64),
		null,
		fromb64(encryptedFileKeyNonceB64),
		policyKey
	);
}
