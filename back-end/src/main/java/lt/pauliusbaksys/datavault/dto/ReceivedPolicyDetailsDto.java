package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.PolicyType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReceivedPolicyDetailsDto(
        UUID policyId,
        String policyName,
        String releasedBy,
        PolicyType releaseType,
        OffsetDateTime releasedAt,
        boolean viewed,
        OffsetDateTime firstAccessAt,
        int accessCount,
        OffsetDateTime lastAccessAt,
        List<FileMetaSummary> policyFiles
) {}
