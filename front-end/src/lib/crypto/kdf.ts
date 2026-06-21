import sodium from 'libsodium-wrappers-sumo';
import { subkeyId64FromId, randomSubkeyId63 } from './encoding';

/**
 * Type (shape of an object) that describes which password-KDF settings we are using.
 * We store them in DB and reuse them later to derive the same key.
 */
export type VaultKdfParams = {
	alg: string;
	opsLimit: number;
	memLimit: number;
	outLen: number;
};

/** KDF_PURPOSE: libsodium KDF context/purpose (exactly 8 ASCII chars).*/
export type VaultContexts = {
	kek: string;
	file: string;
	meta: string;
	hash: string;
};

export function defaultVaultContexts(): VaultContexts {
	return {
		kek: 'VLTKEK01',
		file: 'VLTFIL01',
		meta: 'VLTMET01',
		hash: 'VLTHAS01'
	};
}

// We want the same hash key every time for the same user (so we can detect duplicates).
export const HASH_SUBKEY_ID = 1n;

export async function defaultVaultKdfParams(): Promise<VaultKdfParams> {
	await sodium.ready;
	return {
		alg: 'argon2id',
		opsLimit: sodium.crypto_pwhash_OPSLIMIT_MODERATE, // Iteration count 3
		memLimit: sodium.crypto_pwhash_MEMLIMIT_MODERATE, // Memory (256 MiB)
		outLen: sodium.crypto_kdf_KEYBYTES // 32 byte (256 bit) password-root key
	};
}

/**
 * Deriving a deterministic (not random) subkey from a 32-byte root key using libsodium `crypto_kdf`.
 *
 * @param key32 - 32 byte key KEK ? pwdKey : masterKey.
 * @param purpose8 - 8 character ASCII context (purpose of the key).
 * @param subkeyId - uint64 subkey identifier.
 * @param outLen - Output length in bytes (default: 32).
 * @returns Derived key bytes of length `outLen`.
 */
export async function kdfFromKey(
	key32: Uint8Array,
	purpose8: string,
	subkeyId: bigint | number,
	outLen = 32
): Promise<Uint8Array> {
	await sodium.ready;
	return sodium.crypto_kdf_derive_from_key(outLen, subkeyId, purpose8, key32);
}

/**
 * Deriving key material for a single file from the vault master key.
 *
 * purpose: 'file' returning the per-file 'subKeyId' plus the keys needed to
 * encrypt/decrypt file content and its metadata, and to compute a stable hash key.
 * 
 * purpose: 'meta' returning only the per-file `subKeyId` and `metaKey` (used when
 *   decrypting metadata only).
 *
 * @param masterKey32 32-byte vault master key.
 * @param purpose Which key set to derive: `'file'` or `'meta'`.
 * @param opts Optional derivation settings.
 * @param opts.outLen Output key length in bytes (default: 32).
 * @param opts.context KDF contexts (default: `defaultVaultContexts()`).
 * @param opts.subKeyId unique subKey identifier.
 * @returns Derived key(s) along with `subKeyId`.
 * @throws Error If libsodium is not ready or derivation fails.
 */
type Purpose = 'file' | 'meta';

export async function deriveKeyForFile(
	masterKey32: Uint8Array,
	purpose: 'file',
	subKeyId?: bigint,
	opts?: { outLen?: number; context?: VaultContexts }
): Promise<{ subKeyId: bigint; fileKey: Uint8Array; metaKey: Uint8Array; hashKey: Uint8Array }>;

export async function deriveKeyForFile(
	masterKey32: Uint8Array,
	purpose: 'meta',
	subKeyId: bigint,
	opts?: { outLen?: number; context?: VaultContexts }
): Promise<{ subKeyId: bigint; metaKey: Uint8Array }>;

export async function deriveKeyForFile(
	masterKey32: Uint8Array,
	purpose: Purpose,
	subKeyId?: bigint,
	opts?: { outLen?: number; context?: VaultContexts }
) {
	await sodium.ready;
	const outLen = opts?.outLen ?? 32;
	const contexts = opts?.context ?? defaultVaultContexts();
	const subKeyId_ = subKeyId ?? (await randomSubkeyId63());

	if (purpose === 'file') {
		const [fileKey, metaKey, hashKey] = await Promise.all([
			kdfFromKey(masterKey32, contexts.file, subKeyId_, outLen),
			kdfFromKey(masterKey32, contexts.meta, subKeyId_, outLen),
			kdfFromKey(masterKey32, contexts.hash, HASH_SUBKEY_ID, outLen)
		]);

		return { subKeyId: subKeyId_, fileKey, metaKey, hashKey };
	}
	return {
		subKeyId: subKeyId_,
		metaKey: await kdfFromKey(masterKey32, contexts.meta, subKeyId_, outLen)
	};
}
