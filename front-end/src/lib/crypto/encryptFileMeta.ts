import sodium from 'libsodium-wrappers-sumo';
import { utf8, b64 } from './encoding';

export type FileMeta = {
	fileName: string;
	size: number;
	mimeType: string;
	lastModified: number;
	relativePath?: string;
};

export type EncryptedMeta = {
	metaNonceB64: string;
	metaCipherB64: string;
};

export type Ad = {
	userId: string;
	fileId: string;
	vaultVersion: number;
};


export async function encryptFileMeta(
	meta: FileMeta,
	metaKey: Uint8Array,
	ad: Ad
): Promise<EncryptedMeta> {
	await sodium.ready;

	const msg = sodium.from_string(JSON.stringify(meta));
	const nonce = sodium.randombytes_buf(sodium.crypto_aead_xchacha20poly1305_IETF_NPUBBYTES);
	const add = utf8(`user:${ad.userId}|file:${ad.fileId}|vault_version:${ad.vaultVersion}`);

	const metaCipher = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
		msg,
		add,
		null,
		nonce,
		metaKey
	);

	sodium.memzero(msg);
	return { metaNonceB64: b64(nonce), metaCipherB64: b64(metaCipher) };
}
