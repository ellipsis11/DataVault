package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.enums.AuditLogFilter;
import lt.pauliusbaksys.datavault.model.AuditLog;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.AuditLogRepository;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.io.PrintWriter;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogHashService auditLogHashService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository, AuditLogHashService auditLogHashService, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.auditLogHashService = auditLogHashService;
        this.objectMapper = objectMapper.rebuild()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true).build();
    }

    public void log(UUID userId, AuditLevel level, AuditAction action, String message) {
        log(userId, level, action, message, null);
    }

    public void log(UUID userId, AuditLevel level, AuditAction action, String message, Map<String, Object> metadata) {
        User user = userRepository.findById(userId).orElse(null);
        createLog(user, user != null ? user.getEmail() : null, level, action, message, metadata);
    }

    public void log(User user, AuditLevel level, AuditAction action, String message, Map<String, Object> metadata) {
        createLog(user, user != null ? user.getEmail() : null, level, action, message, metadata);
    }

    public void logAnonymous(String actorEmail, AuditLevel level, AuditAction action, String message) {
        createLog(null, actorEmail, level, action, message, null);
    }

    public void logAnonymous(String actorEmail, AuditLevel level, AuditAction action, String message, Map<String, Object> metadata) {
        createLog(null, actorEmail, level, action, message, metadata);
    }

    private void createLog(
            User user,
            String actorEmail,
            AuditLevel level,
            AuditAction action,
            String message,
            Map<String, Object> metadata
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setActorEmail(actorEmail);
        auditLog.setAuditLevel(level);
        auditLog.setActionType(action);
        auditLog.setMessage(message);
        auditLog.setMetadata(metadata);

        /*
        * createdAt is part of the HMAC body (payload), so we must set it before saving
        * DB DEFAULT now() would be applied too late for hash calculation
        */
        auditLog.setCreatedAt(OffsetDateTime.now());

        byte[] previousHash = auditLogRepository.findTopByOrderByCreatedAtDesc()
                .map(log -> log.getCurrentHash())
                .orElse(null);

        auditLog.setPreviousHash(previousHash);

        String hashBody = buildHashBody(auditLog, previousHash);
        byte[] currentHash = auditLogHashService.calculateHash(hashBody);

        auditLog.setCurrentHash(currentHash);

        auditLogRepository.save(auditLog);
    }

    public AuditLogPage getUserLogs(UUID userId, String query, AuditLogFilter filter, int page, int size){
        PageRequest pageable = PageRequest.of(page, size);

        // Collection is needed here, because we are not passing a single action to the query, but a list/set of actions:
        Collection<AuditAction> actions = (filter == null
                ? EnumSet.allOf(AuditAction.class)
                : filter.getActions());

        Page<AuditLog> logPage = auditLogRepository.searchAuditLogs(userId, query, actions, pageable);

        return new AuditLogPage(
                logPage.getContent().stream().map(
                        logP -> {

                            String body = buildHashBody(logP, logP.getPreviousHash());
                            boolean hashValid = auditLogHashService.verifyHash(body, logP.getCurrentHash());

                            return new AuditLogListItem(
                                    logP.getId(),
                                    logP.getAuditLevel(),
                                    logP.getActionType(),
                                    logP.getMessage(),
                                    logP.getCreatedAt(),
                                    hashValid
                            );
                        }
                ).toList(),
                logPage.getTotalElements()
        );
    }

    public void exportUserLogsCsv(UUID userId, PrintWriter writer) {
        List<AuditLog> auditLogs = auditLogRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);

        writer.write('\uFEFF'); // UTF-8 Byte Order Mark (BOM) for Excel
        writer.println("CreatedAt,Level,Action,Message,HashValid");

        for (AuditLog auditLog : auditLogs){
            String body = buildHashBody(auditLog, auditLog.getPreviousHash());
            boolean hashValid = auditLogHashService.verifyHash(body, auditLog.getCurrentHash());

            writer.printf(
                    "%s,%s,%s,%s,%s%n",
                    csv(formatDateForCsv(auditLog.getCreatedAt())),
                    csv(auditLog.getAuditLevel().name()),
                    csv(auditLog.getActionType().name()),
                    csv(auditLog.getMessage()),
                    csv(hashValid)
            );
        }
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminAuditLogListItemDetails getLogDetailsForAdmin(UUID logId) {
        AuditLog log = auditLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audito žurnalo įrašas nerastas!"));

        UUID userId = log.getUser() != null
                ? log.getUser().getId()
                : null;

        return new AdminAuditLogListItemDetails(
                log.getId(),
                userId,
                log.getActorEmail(),
                log.getAuditLevel(),
                log.getActionType(),
                log.getMessage(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void exportAllLogsForAdmin(PrintWriter writer) {
        List<AuditLog> auditLogs = auditLogRepository.findAll();

        writer.write('\uFEFF'); // UTF-8 Byte Order Mark (BOM) for Excel
        writer.println("CreatedAt,UserId,ActorEmail,Level,Action,Message,MetaData,PreviousHash,CurrentHash,HashValid");

        for (AuditLog auditLog : auditLogs){
            UUID userId = auditLog.getUser() != null
                    ? auditLog.getUser().getId()
                    : null;

            String body = buildHashBody(auditLog, auditLog.getPreviousHash());
            boolean hashValid = auditLogHashService.verifyHash(body, auditLog.getCurrentHash());

            writer.printf(
                    "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csv(formatDateForCsv(auditLog.getCreatedAt())),
                    csv(userId),
                    csv(auditLog.getActorEmail()),
                    csv(auditLog.getAuditLevel().name()),
                    csv(auditLog.getActionType().name()),
                    csv(auditLog.getMessage()),
                    csv(auditLog.getMetadata()),
                    csv(auditLog.getPreviousHash()),
                    csv(auditLog.getCurrentHash()),
                    csv(hashValid)
            );
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminAuditLogPage getAuditLogsPageForAdmin(
            String query,
            AuditLogFilter filter,
            UUID searchingUserId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Collection<String> actions = (filter == null
                ? EnumSet.allOf(AuditAction.class)
                : filter.getActions()
        ).stream()
                .map(Enum::name)
                .toList();

        LocalDate min = LocalDate.of(1970, 1, 1);

        OffsetDateTime from = (fromDate != null)
                ? fromDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                : min.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        OffsetDateTime to = (toDate != null)
                ? toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime() // exclusive
                : OffsetDateTime.now(ZoneOffset.UTC);

        List<AuditLog> logList = auditLogRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));

        boolean logChainIntegrityValid = verifyLogChainIntegrity(logList);

        PageRequest pageable = PageRequest.of(page, size);

        Page<AuditLog> logPage = auditLogRepository.searchAdminAuditLogs(
                query,
                actions,
                searchingUserId,
                from,
                to,
                pageable
        );

        return new AdminAuditLogPage(
                logPage.getContent().stream().map(
                        logP -> {
                            UUID userId = logP.getUser() != null
                                    ? logP.getUser().getId()
                                    : null;

                            String body = buildHashBody(logP, logP.getPreviousHash());
                            boolean hashValid = auditLogHashService.verifyHash(body, logP.getCurrentHash());

                            return new AdminAuditLogListItem(
                                    logP.getId(),
                                    userId,
                                    logP.getActorEmail(),
                                    logP.getAuditLevel(),
                                    logP.getActionType(),
                                    logP.getMessage(),
                                    logP.getCreatedAt(),
                                    hashValid
                            );
                        }
                ).toList(),
                logPage.getTotalElements(),
                logChainIntegrityValid
        );
    }

    private String buildHashBody(AuditLog log, byte[] previousHash) {
        return String.join("|",
                value(log.getUser() != null ? log.getUser().getId() : null),
                value(log.getActorEmail()),
                value(log.getAuditLevel() != null ? log.getAuditLevel().name() : null),
                value(log.getActionType() != null ? log.getActionType().name() : null),
                value(log.getMessage()),
                normalizeMetaData(log.getMetadata()),
                formatCreatedAtForHash(log.getCreatedAt()),
                previousHash == null ? "" : Base64.getEncoder().encodeToString(previousHash)
        );
    }

    private boolean verifyLogChainIntegrity(List<AuditLog> logList){
        if (logList == null || logList.isEmpty()) {
            return true;
        }

        byte[] previousCurrentHash = null;

        for (AuditLog log : logList){
            String body = buildHashBody(log, log.getPreviousHash());
            boolean hashValid = auditLogHashService.verifyHash(body, log.getCurrentHash());

            boolean linkValid = previousCurrentHash == null
                    || MessageDigest.isEqual(log.getPreviousHash(), previousCurrentHash);

            if (!hashValid || !linkValid){
                return false;
            }

            previousCurrentHash = log.getCurrentHash();
        }

        return true;
    }

    private String value(Object value){
        return value == null ? "" : value.toString();
    }

    private String formatDateForCsv(OffsetDateTime createdAt){
        return createdAt == null
                ? ""
                : createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatCreatedAtForHash(OffsetDateTime createdAt){
        return createdAt == null
                ? ""
                : createdAt.toInstant().truncatedTo(ChronoUnit.MILLIS).toString();
    }

    // Normalizing audit log metadata into a stable textual representation.
    private String normalizeMetaData(Object metadata){
        if (metadata == null){
            return "";
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        }
        catch (Exception e){
            throw new IllegalStateException("Nepavyko normalizuoti audito registrų metaduomenų", e);
        }
    }
}