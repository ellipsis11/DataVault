package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.enums.PolicyType;
import lt.pauliusbaksys.datavault.enums.WarningChannel;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PolicyDetailsDto(
        UUID policyId,
        String policyName,
        PolicyType releaseType,
        WarningChannel channel,
        Integer inactivityDays,
        Integer graceDays,
        Integer warningEveryHours,
        OffsetDateTime scheduledReleaseAt,
        Integer warningBeforeDays,
        OffsetDateTime createdAt,
        PolicyStatus policyStatus,
        PolicyType policyType,
        String nextAction,
        List<FileMetaSummary> policyFiles,
        List<PolicyRecipientDto> policyRecipients
) {}
