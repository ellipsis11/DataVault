package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record UserDashboard(
        long totalFiles,
        long totalPolicies,
        long totalReleasedPolicies,
        long totalNewPolicies,
        boolean telegramLinked,
        boolean encryptionKeysGenerated,
        List<AuditLogListItem> auditLogs
) {}