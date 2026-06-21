package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record AdminAuditLogPage(
        List<AdminAuditLogListItem> logList,
        long totalElements,
        boolean logChainIntegrityValid
) {}