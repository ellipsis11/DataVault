package lt.pauliusbaksys.datavault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.enums.PolicyType;
import lt.pauliusbaksys.datavault.enums.WarningChannel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PolicyDto(
        UUID policyId,
        @NotBlank String policyName,
        @NotNull PolicyType releaseType,
        @NotNull WarningChannel channel,
        @NotEmpty List<UUID> recipients,
        @NotEmpty List<UUID> files,
        Integer inactivityDays,
        Integer graceDays,
        Integer warningEveryHours,
        OffsetDateTime scheduledReleaseAt,
        Integer warningBeforeDays,
        OffsetDateTime createdAt,
        PolicyStatus policyStatus,
        String nextAction
) {}