package lt.pauliusbaksys.datavault.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Manifest(
        List<UUID> fileIds,
        UUID policyId,
        UUID recipientUserId,
        String encryptedPolicyKeyForRecipientB64,
        OffsetDateTime createdAt
) {}
