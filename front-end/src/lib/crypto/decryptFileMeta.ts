import sodium from 'libsodium-wrappers-sumo';
import type { Ad, EncryptedMeta, FileMeta } from './encryptFileMeta';
import { fromb64, utf8 } from './encoding';

export async function decryptFileMeta(
	enc: EncryptedMeta,
	metaKey: Uint8Array,
	ad: Ad
): Promise<FileMeta> {
	await sodium.ready;
	const add = utf8(`user:${ad.userId}|file:${ad.fileId}|vault_version:${ad.vaultVersion}`);
	const nonce = fromb64(enc.metaNonceB64);
	const meta_cipher = fromb64(enc.metaCipherB64);

	const plain_meta = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
		null,
		meta_cipher,
		add,
		nonce,
		metaKey
	);
	const meta = JSON.parse(sodium.to_string(plain_meta)) as FileMeta;
	sodium.memzero(plain_meta);
	return meta;
}
