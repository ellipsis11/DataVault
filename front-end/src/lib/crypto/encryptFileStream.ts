import sodium from 'libsodium-wrappers-sumo';
import { utf8 } from './encoding';

export async function encryptFileStream(
	fileKey: Uint8Array,
	file: File,
	userId: string,
	vaultVersion: number
): Promise<{
	fileId: string;
	cipherBlob: Blob;
}> {
	// Ensuring the crypto functions are usable then we call them
	await sodium.ready;

	/**
	 * init_push returns:
	 * - state: internal secretstream encrypt state; reuse for every chunk
	 * - header: non-secret setup bytes; required to start decrypt; store/send with ciphertext
	 * key is secret; header isn’t secret but is mandatory.
	 */
	const { state, header } = sodium.crypto_secretstream_xchacha20poly1305_init_push(fileKey);

	/**
	 * file.stream() returns a ReadableStream for the file (chunk-by-chunk).
	 * getReader() returns a reader with reader.read() to pull the next chunk.
	 */
	const reader = file.stream().getReader();

	/**
	 * For collecting output chunks.
	 * Starting with `header` so the final encrypted Blob begins with the header.
	 */
	const headerU8 = new Uint8Array(header);
	const parts: BlobPart[] = [headerU8];

	/**Aditional data*/
	const fileId = crypto.randomUUID();
	const ad = utf8(`user:${userId}|file:${fileId}|vault_version:${vaultVersion}`);

	/**
	 * Prefixing each ciphertext chunk with a fixed 4-byte uint32 length.
	 * Needed because decrypt must pass pull() one full message, but Blob/streams lose chunk boundaries.
	 * len4(n) encodes n (the value) as 4 little-endian bytes so decrypt can read 4 bytes → n → then read exactly n bytes.
	 */
	const len4 = (n: number) => {
		const b = new ArrayBuffer(4);
		new DataView(b).setUint32(0, n, true);
		return new Uint8Array(b);
	};

	/**
	 * Reads the first chunk upfront.
	 * Secretstream needs different tags:
	 * - middle chunks: TAG_MESSAGE
	 * - last chunk:   TAG_FINAL
	 * You only know a chunk is the last after attempting to read the next one,
	 * so this uses a 1-chunk lookahead strategy.
	 */
	const r = await reader.read();

	/** IF file empty: still writing a final chunk (TAG_FINAL) so the stream closes cleanly. */
	if (r.done) {
		const last = sodium.crypto_secretstream_xchacha20poly1305_push(
			state,
			new Uint8Array(),
			ad,
			sodium.crypto_secretstream_xchacha20poly1305_TAG_FINAL
		);
		const c = new Uint8Array(last);
		parts.push(len4(c.length), c);

		return { fileId, cipherBlob: new Blob(parts) };
	}

	let current = r.value;
	while (true) {
		const next = await reader.read();
		const isLast = next.done;
		const tag = isLast
			? sodium.crypto_secretstream_xchacha20poly1305_TAG_FINAL
			: sodium.crypto_secretstream_xchacha20poly1305_TAG_MESSAGE;
		const encChunk = sodium.crypto_secretstream_xchacha20poly1305_push(state, current, ad, tag);

		const c = new Uint8Array(encChunk);
		parts.push(len4(c.length), c);

		if (isLast) break;
		current = next.value;
	}

	/** Returning final encrypted Blob as: [header, encChunk1, encChunk2, ..., encFinalChunk]. */
	return { fileId, cipherBlob: new Blob(parts) };
}
