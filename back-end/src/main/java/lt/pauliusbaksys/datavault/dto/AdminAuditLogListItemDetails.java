package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AdminAuditLogListItemDetails(
        UUID id,
        UUID userId,
        String actorEmail,
        AuditLevel level,
        AuditAction actionType,
        String message,
        Map<String, Object> metaData,
        OffsetDateTime createdAt
) {}