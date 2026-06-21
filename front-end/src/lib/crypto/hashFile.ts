import sodium from 'libsodium-wrappers-sumo';
import { b64 } from './encoding';

/**
 * For dedupe – it can detect duplicates, but i can leak info (someone with a guessed file can hash it and compare).
 * */
export async function sha256(file: File): Promise<string> {
	await sodium.ready;

	const state = sodium.crypto_hash_sha256_init();
	const reader = file.stream().getReader();

	while (true) {
		const { value, done } = await reader.read(); // Get next chunck
		if (done) break;
		sodium.crypto_hash_sha256_update(state, value); // feed chunck into hash
	}

	// Finishing SHA-256 hashing and returning the final hash output
	const digest = sodium.crypto_hash_sha256_final(state);
	return sodium.to_hex(digest);
}

/**
 * Computing a stable, keyed content fingerprint of a file and return it as Base64.
 *
 * This uses libsodium's `crypto_generichash` (BLAKE2b) in keyed mode, which acts like an HMAC:
 * the output is deterministic for the same `(hashKey32, file bytes)` but looks random without the key.
 *
 * Why keyed hashing?
 * - Enables duplicate detection (same plaintext file -> same hash) without storing a raw plaintext hash.
 * - Prevents leaking information that an unkeyed hash (SHA-256 of plaintext) could reveal.
 *
 * Why streaming?
 * - The file is read chunk-by-chunk using `file.stream()` so large files do not need to be loaded
 *   fully into memory (unlike `await file.arrayBuffer()`).
 */
export async function keyedContentHashB64(file: File, hashKey32: Uint8Array): Promise<string> {
	await sodium.ready;
	const outLen = 32;
	const state = sodium.crypto_generichash_init(hashKey32, outLen);
	const reader = file.stream().getReader();

	while (true) {
		const { value, done } = await reader.read();
		if (done) break;
		sodium.crypto_generichash_update(state, value);
	}

	const out = sodium.crypto_generichash_final(state, outLen);
	return b64(out);
}
