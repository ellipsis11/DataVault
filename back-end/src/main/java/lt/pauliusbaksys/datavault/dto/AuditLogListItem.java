package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogListItem(
        UUID id,
        AuditLevel level,
        AuditAction actionType,
        String message,
        OffsetDateTime createdAt,
        Boolean hashValid
) {}