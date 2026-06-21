package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record AdminDashboard(
        long totalUsers,
        long totalFiles,
        long totalPolicies,
        long totalAuditLogs,
        List<AdminAuditLogListItem> auditLogs,
        long newUsersToday,
        long newFilesToday,
        long newPoliciesToday
) {}