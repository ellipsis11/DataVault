import sodium from 'libsodium-wrappers-sumo';

/**
 * Converts raw binary bytes to base64 string so you we can send them in JSON.
 * Libsodium’s “original” Base64 format */
export function b64(bytes: Uint8Array): string {
	return sodium.to_base64(bytes, sodium.base64_variants.ORIGINAL);
}

export function fromb64(s: string): Uint8Array {
	return sodium.from_base64(s, sodium.base64_variants.ORIGINAL);
}

/** Converts a JS string to UTF-8 bytes (because sodium APIs operate on bytes, not JS strings). */
export function utf8(s: string): Uint8Array {
	return sodium.from_string(s);
}

/**
 * UUID/string -> uint64 subkey_id (BigInt).
 * We need a deterministic 64 bit "subkey id" for key-derivation APIs (KDF subkey_id).
 * Taking only part of the UUID directly can be biased/format-dependent hashing gives a uniform-looking 64-bit value.
 * We hash the string -> 8 bytes, then pack those 8 bytes into a uint64 (as BigInt).
 */
export async function subkeyId64FromId(id: string): Promise<bigint> {
	await sodium.ready;
	const h8 = sodium.crypto_generichash(8, utf8(id), null); // 8 bytes
	h8[7] &= 0x7f; // keep it in signed 63-bit range
	let x = 0n;
	for (let i = 0; i < 8; i++) x |= BigInt(h8[i]) << (8n * BigInt(i));
	return x;
}

export async function randomSubkeyId63(): Promise<bigint> {
	await sodium.ready;
	const b = sodium.randombytes_buf(8);
	b[7] &= 0x7f; // 63-bit safe
	let x = 0n;
	for (let i = 0; i < 8; i++) x |= BigInt(b[i]) << (8n * BigInt(i));
	return x;
}
