package lt.pauliusbaksys.datavault.dto;

import java.util.UUID;

public record RecipientPolicyAccess(
        UUID recipientUserId,
        String encryptedPolicyKeyForRecipientB64,
        Manifest manifest,
        String signatureByOwnerB64
) {
}
