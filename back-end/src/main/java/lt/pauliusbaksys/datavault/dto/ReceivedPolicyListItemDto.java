package lt.pauliusbaksys.datavault.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceivedPolicyListItemDto(
        UUID policyId,
        String policyName,
        String releasedBy,
        int filesCount,
        OffsetDateTime releasedAt,
        boolean viewed
) {}
