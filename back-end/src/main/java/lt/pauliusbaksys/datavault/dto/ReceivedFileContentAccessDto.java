package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record ReceivedFileContentAccessDto(
        UUID policyId,
        UUID fileId,
        UUID ownerUserId,
        String encryptedPolicyKeyForRecipientB64,
        String encryptedFileKeyByPolicyB64,
        String encryptedFileKeyNonceB64,
        Manifest manifest,
        String signatureByOwnerB64,
        String ownerSignPublicKeyB64
) {}