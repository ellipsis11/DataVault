package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletResponse;
import lt.pauliusbaksys.datavault.dto.AdminAuditLogListItemDetails;
import lt.pauliusbaksys.datavault.dto.AdminAuditLogPage;
import lt.pauliusbaksys.datavault.enums.AuditLogFilter;
import lt.pauliusbaksys.datavault.service.AuditLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public AdminAuditLogPage getAuditLogs(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) AuditLogFilter filter,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return auditLogService.getAuditLogsPageForAdmin(
                query.trim(),
                filter,
                userId,
                fromDate,
                toDate,
                page,
                size
        );
    }

    @GetMapping("/{logId}")
    public AdminAuditLogListItemDetails getAuditLogDetails(@PathVariable UUID logId) {
        return auditLogService.getLogDetailsForAdmin(logId);
    }

    @GetMapping("/export")
    public void exportAuditLogs(HttpServletResponse res) throws IOException {
        res.setContentType("text/csv");
        res.setCharacterEncoding("UTF-8");

        // Writing the CSV file directly to the HttpServletResponse.
        auditLogService.exportAllLogsForAdmin(res.getWriter());
    }
}
