package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record UserSignPrivateKey(
        UUID userId,
        String kdfSaltB64,
        KdfParams kdfParams,
        int vaultVersion,
        KdfContexts kdfContexts,
        String ownerSignPrivateKeyEncryptedB64,
        String ownerSignPrivateKeyWrapNonceB64
) {}
