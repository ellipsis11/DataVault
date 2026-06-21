package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lt.pauliusbaksys.datavault.dto.AuditLogPage;
import lt.pauliusbaksys.datavault.dto.PolicyPage;
import lt.pauliusbaksys.datavault.enums.AuditLogFilter;
import lt.pauliusbaksys.datavault.service.AuditLogService;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AuditLogController(AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public AuditLogPage getLogs(
            HttpServletRequest req,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) AuditLogFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ){
        return auditLogService.getUserLogs(currentUserService.getCurrentUserId(req), query.trim(), filter, page, size);
    }

    /*
     * We are not using Resource here, because we are generating text dynamically.
     * Not reading an existing file from disk.
     * Writing the file directly into HttpServletResponse.
     */
    @GetMapping("/export")
    public void exportLogs(HttpServletRequest req, HttpServletResponse res)throws IOException {
        res.setContentType("text/csv");
        res.setCharacterEncoding("UTF-8");

        // Writing the CSV file directly to the HttpServletResponse.
        auditLogService.exportUserLogsCsv(currentUserService.getCurrentUserId(req), res.getWriter());
    }
}
