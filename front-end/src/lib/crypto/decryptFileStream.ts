import sodium from 'libsodium-wrappers-sumo';
import { ByteReader } from './byteReader';
import { utf8 } from './encoding';

export async function decryptFileStream(
	cipherBlob: Blob,
	fileKey: Uint8Array,
	userId: string,
	fileId: string,
	vault_version: number
): Promise<Blob> {
	await sodium.ready;

	/** Getting header size in bytes. */
	const HEADERBYTES = sodium.crypto_secretstream_xchacha20poly1305_HEADERBYTES;

	const reader = cipherBlob.stream().getReader();
	const br = new ByteReader(reader);

	const header = await br.readExact(HEADERBYTES);
	if (!header) throw new Error('Missing header');

	/** Initting decrypt state from header + key (required before pulling/decrypting chunks). */
	const state = sodium.crypto_secretstream_xchacha20poly1305_init_pull(header, fileKey);

	const parts: BlobPart[] = [];
	let sawFinal = false;

	const ad = utf8(`user:${userId}|file:${fileId}|vault_version:${vault_version}`);

	while (true) {
		const lenBytes = await br.readExact(4);
		if (!lenBytes) break;

		const len = new DataView(lenBytes.buffer, lenBytes.byteOffset, 4).getUint32(0, true);

		const chunk = await br.readExact(len);
		if (!chunk) throw new Error('Ciphertext truncated (missing chunk bytes)');

		/** Decrypt one ciphertext chunk (and verify auth) returns { message, tag }. */
		const res = sodium.crypto_secretstream_xchacha20poly1305_pull(state, chunk, ad);
		if (res === false) throw new Error('Decryption failed (bad key or corrupted data)!');

		parts.push(new Uint8Array(res.message));

		/** Stopping when the final chunk tag is reached (end of stream). */
		if (res.tag === sodium.crypto_secretstream_xchacha20poly1305_TAG_FINAL) {
			sawFinal = true;
			break;
		}
	}

	if (!sawFinal) throw new Error('Ciphertext truncated (missing FINAL tag).');

	/** Rebuilding the decrypted output as a single Blob from all collected chunks (BlobParts). */
	return new Blob(parts);
}
