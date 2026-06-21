package lt.pauliusbaksys.datavault.dto;

import java.util.List;

public record AuditLogPage(
        List<AuditLogListItem> logList,
        long totalElements
) {}