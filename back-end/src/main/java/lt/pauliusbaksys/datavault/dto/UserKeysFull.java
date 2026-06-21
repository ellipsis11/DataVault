package lt.pauliusbaksys.datavault.dto;

public record UserKeysFull(
        String kdfSaltB64,
        KdfParams kdfParams,
        String wrappedMasterKeyB64,
        String nonceB64,
        int vaultVersion,
        KdfContexts kdfContexts,
        String recipientPublicKeyB64,
        String recipientPrivateKeyEncryptedB64,
        String recipientPrivateKeyWrapNonceB64,
        String ownerSignPublicKeyB64,
        String ownerSignPrivateKeyEncryptedB64,
        String ownerSignPrivateKeyWrapNonceB64
) {}
