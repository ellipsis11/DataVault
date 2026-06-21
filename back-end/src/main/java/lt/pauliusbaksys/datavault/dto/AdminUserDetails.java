package lt.pauliusbaksys.datavault.dto;

import lt.pauliusbaksys.datavault.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserDetails(
        UUID id,
        String email,
        UserRole role,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt,

        boolean telegramLinked,
        String telegramUsername,
        OffsetDateTime telegramConnectedAt,

        boolean keysGenerated,
        OffsetDateTime keysGeneratedAt,

        long logCount,
        long ownedPolicyCount,
        long receivedPolicyCount,
        long uploadedFileCount,

        List<AuditLogListItem> recentLogs
) {}