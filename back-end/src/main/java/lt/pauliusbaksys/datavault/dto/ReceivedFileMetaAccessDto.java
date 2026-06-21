package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record ReceivedFileMetaAccessDto(
        UUID policyId,
        UUID fileId,
        UUID ownerUserId,
        String encryptedPolicyKeyForRecipientB64,
        String encryptedMetaKeyByPolicyB64,
        String encryptedMetaKeyNonceB64,
        String metaCipherB64,
        String metaNonceB64,
        Manifest manifest,
        String signatureByOwnerB64,
        String ownerSignPublicKeyB64
) {}