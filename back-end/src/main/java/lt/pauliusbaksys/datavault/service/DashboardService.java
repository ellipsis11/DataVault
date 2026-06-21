package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.model.AuditLog;
import lt.pauliusbaksys.datavault.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final FileStorageRepository fileStorageRepository;
    private final ConditionalReleaseRepository conditionalReleaseRepository;
    private final PolicyRecipientRepository policyRecipientRepository;
    private final UserTelegramLinkRepository userTelegramLinkRepository;
    private final UserKeyRepository userKeyRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public DashboardService(FileStorageRepository fileStorageRepository, ConditionalReleaseRepository conditionalReleaseRepository, PolicyRecipientRepository policyRecipientRepository, UserTelegramLinkRepository userTelegramLinkRepository, UserKeyRepository userKeyRepository, AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.fileStorageRepository = fileStorageRepository;
        this.conditionalReleaseRepository = conditionalReleaseRepository;
        this.policyRecipientRepository = policyRecipientRepository;
        this.userTelegramLinkRepository = userTelegramLinkRepository;
        this.userKeyRepository = userKeyRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public UserDashboard getDashboardDataForUser(UUID userId) {
        long totalFiles  = fileStorageRepository.countByOwner_Id(userId);
        long totalPolicies  = conditionalReleaseRepository.countByOwner_Id(userId);
        long totalReleasedPolicies = policyRecipientRepository.countByRecipient_IdAndPolicy_Status(userId, PolicyStatus.RELEASED);
        long totalNewPolicies  = policyRecipientRepository.countNewReleasedPolicies(userId, PolicyStatus.RELEASED);
        boolean telegramLinked = userTelegramLinkRepository.existsById(userId);
        boolean keysGenerated = userKeyRepository.existsByUserId(userId);
        List<AuditLog> auditLogs = auditLogRepository.findTop10ByUser_IdOrderByCreatedAtDesc(userId);

        return new UserDashboard(
                totalFiles,
                totalPolicies,
                totalReleasedPolicies,
                totalNewPolicies,
                telegramLinked,
                keysGenerated,
                auditLogs.stream().map(
                        a -> new AuditLogListItem(
                            a.getId(),
                            a.getAuditLevel(),
                            a.getActionType(),
                            a.getMessage(),
                            a.getCreatedAt(),
                                null
                        )
                ).toList()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard getDashboardDataForAdmin() {
        long totalUsers = userRepository.count();
        long totalFiles = fileStorageRepository.count();
        long totalPolicies = conditionalReleaseRepository.count();
        long totalAuditLogs = auditLogRepository.count();
        List<AuditLog> auditLogs = auditLogRepository.findTop10ByOrderByCreatedAtDesc();

        OffsetDateTime todayStart = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime();

        OffsetDateTime tomorrowStart = LocalDate.now(ZoneOffset.UTC)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime();

        long newUsersToday = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(todayStart, tomorrowStart);
        long newFilesToday = fileStorageRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(todayStart, tomorrowStart);
        long newPoliciesToday = conditionalReleaseRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(todayStart, tomorrowStart);

        return new AdminDashboard(
                totalUsers,
                totalFiles,
                totalPolicies,
                totalAuditLogs,
                auditLogs.stream().map(
                        a -> {
                            UUID userId = a.getUser() != null ? a.getUser().getId() : null;
                            return new AdminAuditLogListItem(
                                    a.getId(),
                                    userId,
                                    a.getActorEmail(),
                                    a.getAuditLevel(),
                                    a.getActionType(),
                                    a.getMessage(),
                                    a.getCreatedAt(),
                                    null
                            );
                        }
                ).toList(),
                newUsersToday,
                newFilesToday,
                newPoliciesToday
        );
    }
}
