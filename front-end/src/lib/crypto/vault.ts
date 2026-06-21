import sodium from 'libsodium-wrappers-sumo';
import { b64, fromb64, utf8 } from './encoding';
import { DecryptionFailedError } from '$lib/error';
import {
	defaultVaultKdfParams,
	defaultVaultContexts,
	kdfFromKey,
	type VaultKdfParams,
	type VaultContexts
} from './kdf';

export type VaultRecordBasic = {
	kdfSaltB64: string;
	kdfParams: VaultKdfParams;
	wrappedMasterKeyB64: string;
	nonceB64: string;
	vaultVersion: number;
	kdfContexts: VaultContexts;
};

export type VaultRecordBasicForSigning = {
	userId: string;
	kdfSaltB64: string;
	kdfParams: VaultKdfParams;
	vaultVersion: number;
	kdfContexts: VaultContexts;
	ownerSignPrivateKeyEncryptedB64: string;
	ownerSignPrivateKeyWrapNonceB64: string;
};

type VaultRecordToStore = VaultRecordBasic & {
	recipientPublicKeyB64: string;
	recipientPrivateKeyEncryptedB64: string;
	recipientPrivateKeyWrapNonceB64: string;
	ownerSignPublicKeyB64: string;
	ownerSignPrivateKeyEncryptedB64: string;
	ownerSignPrivateKeyWrapNonceB64: string;
};

export type RecipientUnlockRecord = {
	userId: string;
	kdfSaltB64: string;
	kdfParams: VaultKdfParams;
	vaultVersion: number;
	kdfContexts: VaultContexts;
	recipientPublicKeyB64: string;
	recipientPrivateKeyEncryptedB64: string;
	recipientPrivateKeyWrapNonceB64: string;
	ownerSignPublicKeyB64: string;
};

const MASTER_KEY_BYTES = 32;

/**
 * Creates a new vault record from a user secret phrase.
 *
 * @param password - User secret phrase used to derive kdf keys, what will encrypt the masterKey.
 * @param opts - Optional settings (you can omit it and defaults will be used).
 * @param opts.vaultVersion - Vault format/version to write (defaults to current).
 * @param opts.kdfParams - KDF settings (algorithm/ops/mem/salt, etc.) defaults to recommended params.
 * @param opts.contexts - App-specific context strings for key derivation defaults to built-in contexts.
 * @param opts.aad - Optional AAD (additional authenticated data) to bind metadata (userId/vaultId) to ciphertext.
 * @returns The created vault record (encrypted payload + metadata).
 */
export async function createVaultRecord(
	password: string,
	userId: string,
	opts?: {
		vaultVersion?: number;
		kdfParams?: VaultKdfParams;
		contexts?: VaultContexts;
		ad?: string;
	}
): Promise<VaultRecordToStore> {
	await sodium.ready;
	const vault_version = opts?.vaultVersion ?? 1;
	const kdf_params = opts?.kdfParams ?? (await defaultVaultKdfParams());
	const contexts = opts?.contexts ?? defaultVaultContexts();
	const kdf_salt = sodium.randombytes_buf(sodium.crypto_pwhash_SALTBYTES); // 128-bit salt

	/** Password derived Key (hashing password for KDF using argon2id)*/
	const pwdKey = sodium.crypto_pwhash(
		kdf_params.outLen,
		password,
		kdf_salt,
		kdf_params.opsLimit,
		kdf_params.memLimit,
		sodium.crypto_pwhash_ALG_ARGON2ID13
	);

	const kek = await kdfFromKey(pwdKey, contexts.kek, 1);

	const extra = opts?.ad ? `|${opts.ad}` : '';
	const ad = utf8(`user:${userId}|vault_version:${vault_version}${extra}`);

	const masterKey = sodium.randombytes_buf(MASTER_KEY_BYTES);

	/**
	 * Encrypts the masterKey with KEK
	 * 24-bytes (192-bit) nonce for wrapping/encryption scheme
	 */
	const { wrappedKey, nonce } = wrapKey(masterKey, kek, ad);

	const {
		recipientPublicKeyB64,
		recipientPrivateKeyEncryptedB64,
		recipientPrivateKeyWrapNonceB64
	} = generateRecipientAsymKeys(kek, ad);

	const {
		ownerSignPublicKeyB64,
		ownerSignPrivateKeyEncryptedB64,
		ownerSignPrivateKeyWrapNonceB64
	} = generateOwnerSigningKeys(kek, ad);

	/**
	 * Wipes the key bytes from memory (overwrites the buffer with zeros).
	 * To reduce the chance that these values stays in RAM after we done using it. */
	sodium.memzero(masterKey);
	sodium.memzero(pwdKey);
	sodium.memzero(kek);

	return {
		kdfSaltB64: b64(kdf_salt),
		kdfParams: kdf_params,
		wrappedMasterKeyB64: b64(wrappedKey),
		nonceB64: b64(nonce),
		vaultVersion: vault_version,
		kdfContexts: contexts,
		recipientPublicKeyB64,
		recipientPrivateKeyEncryptedB64,
		recipientPrivateKeyWrapNonceB64,
		ownerSignPublicKeyB64,
		ownerSignPrivateKeyEncryptedB64,
		ownerSignPrivateKeyWrapNonceB64
	};
}

export async function unwrapKey(
	secretPhrase: string,
	userId: string,
	record: VaultRecordBasic | VaultRecordBasicForSigning | RecipientUnlockRecord,
	ad?: string
): Promise<Uint8Array> {
	await sodium.ready;

	const contexts = record.kdfContexts;
	const kdf_salt = fromb64(record.kdfSaltB64);
	let wrap_nonce: Uint8Array;
	let keyToUnwrap: Uint8Array;

	// Determining which key and nonce to use based on the fetched from API record shape (basic vault record, signing keys record, or recipient unlock record)
	if ('nonceB64' in record) {
		wrap_nonce = fromb64(record.nonceB64);
		keyToUnwrap = fromb64(record.wrappedMasterKeyB64);
	} else if ('ownerSignPrivateKeyWrapNonceB64' in record) {
		wrap_nonce = fromb64(record.ownerSignPrivateKeyWrapNonceB64);
		keyToUnwrap = fromb64(record.ownerSignPrivateKeyEncryptedB64);
	} else if ('recipientPrivateKeyWrapNonceB64' in record) {
		wrap_nonce = fromb64(record.recipientPrivateKeyWrapNonceB64);
		keyToUnwrap = fromb64(record.recipientPrivateKeyEncryptedB64);
	} else {
		throw new Error('Unsupported vault record shape!');
	}

	const pwdKey = sodium.crypto_pwhash(
		record.kdfParams.outLen,
		secretPhrase,
		kdf_salt,
		record.kdfParams.opsLimit,
		record.kdfParams.memLimit,
		sodium.crypto_pwhash_ALG_ARGON2ID13
	);

	const kek = await kdfFromKey(pwdKey, contexts.kek, 1);
	const extra = ad ? `|${ad}` : '';
	const add = utf8(`user:${userId}|vault_version:${record.vaultVersion}${extra}`);

	try {
		return sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
			null,
			keyToUnwrap,
			add,
			wrap_nonce,
			kek
		);
	} catch (e) {
		if (e instanceof TypeError) throw e;
		throw new DecryptionFailedError(
			'Nepavyko iššifruoti pagrindinio rakto. Tikėtina, kad įvestas neteisingas šifravimo slaptažodis.'
		);
	} finally {
		sodium.memzero(pwdKey);
		sodium.memzero(kek);
	}
}

function generateRecipientAsymKeys(
	kek: Uint8Array,
	ad: Uint8Array
): {
	recipientPublicKeyB64: string;
	recipientPrivateKeyEncryptedB64: string;
	recipientPrivateKeyWrapNonceB64: string;
} {
	// Curve25519 / X25519 keypair for encryption
	const keyPair = sodium.crypto_box_keypair();
	const { wrappedKey, nonce } = wrapKey(keyPair.privateKey, kek, ad);
	sodium.memzero(keyPair.privateKey);

	return {
		recipientPublicKeyB64: b64(keyPair.publicKey),
		recipientPrivateKeyEncryptedB64: b64(wrappedKey),
		recipientPrivateKeyWrapNonceB64: b64(nonce)
	};
}

function generateOwnerSigningKeys(
	kek: Uint8Array,
	ad: Uint8Array
): {
	ownerSignPublicKeyB64: string;
	ownerSignPrivateKeyEncryptedB64: string;
	ownerSignPrivateKeyWrapNonceB64: string;
} {
	// Ed25519 keypair for signatures
	const keyPair = sodium.crypto_sign_keypair();
	const { wrappedKey, nonce } = wrapKey(keyPair.privateKey, kek, ad);
	sodium.memzero(keyPair.privateKey);

	return {
		ownerSignPublicKeyB64: b64(keyPair.publicKey),
		ownerSignPrivateKeyEncryptedB64: b64(wrappedKey),
		ownerSignPrivateKeyWrapNonceB64: b64(nonce)
	};
}

function wrapKey(
	keyToWrap: Uint8Array,
	kek: Uint8Array,
	ad: Uint8Array
): { wrappedKey: Uint8Array; nonce: Uint8Array } {
	const nonce = sodium.randombytes_buf(sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES);
	const wrappedKey = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
		keyToWrap,
		ad,
		null,
		nonce,
		kek
	);

	return { wrappedKey, nonce };
}
