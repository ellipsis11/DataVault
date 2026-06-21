package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record RecipientUnlockKeys(
        UUID userId,
        String kdfSaltB64,
        KdfParams kdfParams,
        int vaultVersion,
        KdfContexts kdfContexts,
        String recipientPublicKeyB64,
        String recipientPrivateKeyEncryptedB64,
        String recipientPrivateKeyWrapNonceB64,
        String ownerSignPublicKeyB64
) {}
