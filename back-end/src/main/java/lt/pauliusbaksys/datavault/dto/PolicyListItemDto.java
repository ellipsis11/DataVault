package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.enums.PolicyType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyListItemDto(
    UUID policyId,
    String policyName,
    int filesCount,
    int recipientsCount,
    PolicyStatus policyStatus,
    String nextAction,
    PolicyType policyType,
    OffsetDateTime createdAt
) {}
