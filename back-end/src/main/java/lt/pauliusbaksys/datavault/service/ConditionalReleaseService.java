package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.*;
import lt.pauliusbaksys.datavault.model.*;
import lt.pauliusbaksys.datavault.model.Policy;
import lt.pauliusbaksys.datavault.repository.*;
import lt.pauliusbaksys.datavault.telegram.PolicyNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ConditionalReleaseService {

    private final ConditionalReleaseRepository conditionalReleaseRepository;
    private final PolicyRecipientRepository policyRecipientRepository;
    private final PolicyFileRepository policyFileRepository;
    private final UserRepository userRepository;
    private final FileStorageRepository fileStorageRepository;
    private final UserTelegramLinkRepository userTelegramLinkRepository;
    private final UserKeyRepository userKeyRepository;
    private final PolicyNotificationService policyNotificationService;
    private final AuditLogService auditLogService;
    private final Logger logger = LoggerFactory.getLogger(ConditionalReleaseService.class);

    public ConditionalReleaseService(ConditionalReleaseRepository conditionalReleaseRepository, PolicyRecipientRepository policyRecipientRepository, PolicyFileRepository policyFileRepository, UserRepository userRepository, FileStorageRepository fileStorageRepository, UserTelegramLinkRepository userTelegramLinkRepository, UserKeyRepository userKeyRepository, PolicyNotificationService policyNotificationService, AuditLogService auditLogService) {
        this.conditionalReleaseRepository = conditionalReleaseRepository;
        this.policyRecipientRepository = policyRecipientRepository;
        this.policyFileRepository = policyFileRepository;
        this.userRepository = userRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.userTelegramLinkRepository = userTelegramLinkRepository;
        this.userKeyRepository = userKeyRepository;
        this.policyNotificationService = policyNotificationService;
        this.auditLogService = auditLogService;
    }

    // Ensures all policy update database operations succeed or roll back together
    @Transactional
    public void createNewConditionalReleasePolicy(UUID userId, NewPolicy newPolicy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas!"));

        if (!userTelegramLinkRepository.existsById(user.getId())) {
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_CREATE,
                    "Sąlyginio atskleidimo politikos sukurti nepavyko: Telegram paskyra nesusieta",
                    Map.of("reason", "TELEGRAM_NOT_LINKED")
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Prieš kuriant sąlyginio atskleidimo politiką reikia susieti Telegram paskyrą."
            );
        }

        try {
            PolicyDto newPolicy_ = newPolicy.policy();

            // Taking DTO data from newPolicy, verifing that the referenced files and recipients exist
            ResolvedPolicyData resolved = resolvePolicyData(newPolicy);

            Policy policy = switch (newPolicy_.releaseType()){
                case INACTIVITY -> createInactivityPolicy(user, newPolicy_);
                case DATE_TIME -> createDateTimePolicy(user, newPolicy_);
                case MANUAL_RELEASE -> createManualReleasePolicy(user, newPolicy_);
            };

            Policy savedPolicy = conditionalReleaseRepository.save(policy);
            savePolicyRelations(savedPolicy, newPolicy, resolved);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.POLICY_CREATE,
                    "Sąlyginio atskleidimo politika sėkmingai sukurta",
                    Map.of(
                            "policyId", savedPolicy.getId(),
                            "policyName", savedPolicy.getPolicyName(),
                            "policyType", savedPolicy.getType()
                    )
            );
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_CREATE,
                    "Sąlyginio atskleidimo politikos sukurti nepavyko",
                    Map.of(
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    };

    private Policy createInactivityPolicy(User user, PolicyDto newPolicy) {
        inactivityPolicyCheck(newPolicy);

        Policy policy = new Policy(
                newPolicy.policyId(),
                user,
                newPolicy.policyName(),
                newPolicy.releaseType(),
                newPolicy.inactivityDays(),
                newPolicy.graceDays(),
                null,
                newPolicy.warningEveryHours(),
                null,
                newPolicy.createdAt()
        );

        policy.setLastHeartbeatAt(OffsetDateTime.now());

        return policy;
    }

    private void inactivityPolicyCheck(PolicyDto newPolicy){
        if (newPolicy.inactivityDays() == null || newPolicy.inactivityDays() <= 0) {
            throw new IllegalArgumentException("Neaktyvumo dienų skaičius turi būti didesnis nei 0");
        }

        if (newPolicy.graceDays() != null && newPolicy.graceDays() <= 0) {
            throw new IllegalArgumentException("Atidėjimo laikotarpio dienų skaičius turi būti didesnis nei 0");
        }

        if (newPolicy.graceDays() != null &&
                (newPolicy.warningEveryHours() == null || newPolicy.warningEveryHours() <= 0)) {
            throw new IllegalArgumentException("Įspėjimų siuntimo intervalas valandomis turi būti didesnis nei 0, kai įjungtas atidėjimo laikotarpis");
        }

        if (newPolicy.graceDays() == null && newPolicy.warningEveryHours() != null) {
            throw new IllegalArgumentException("Įspėjimų siuntimo intervalas valandomis gali būti nustatytas tik tada, kai įjungtas atidėjimo laikotarpis");
        }
    }

    private Policy createDateTimePolicy(User user, PolicyDto newPolicy) {
        dateTimePolicyCheck(newPolicy);

        return new Policy(
                newPolicy.policyId(),
                user,
                newPolicy.policyName(),
                newPolicy.releaseType(),
                null,
                null,
                newPolicy.scheduledReleaseAt(),
                null,
                newPolicy.warningBeforeDays(),
                newPolicy.createdAt()
        );
    }

    private void dateTimePolicyCheck(PolicyDto policy){
        if (policy.scheduledReleaseAt() == null){
            throw new IllegalArgumentException("Suplanuota atskleidimo data turi būti nurodyta.");
        }
        if (policy.warningBeforeDays() != null && policy.warningBeforeDays() <= 0){
            throw new IllegalArgumentException("Įspėjimo prieš atskleidimą dienų skaičius turi būti didesnis nei 0");
        }
    }

    private Policy createManualReleasePolicy(User user, PolicyDto newPolicy) {
        return new Policy(
                newPolicy.policyId(),
                user,
                newPolicy.policyName(),
                newPolicy.releaseType(),
                null,
                null,
                null,
                null,
                null,
                newPolicy.createdAt()
        );
    }

    public PolicyPage getPoliciesPage(UUID userId, String query, PolicyStatus filter, int page, int size){
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Policy> policyPage;

        if (filter == null) {
            policyPage = conditionalReleaseRepository
                    .findAllByOwner_IdAndPolicyNameContainingIgnoreCase(userId, query, pageable);
        } else {
            policyPage = conditionalReleaseRepository
                    .findAllByOwner_IdAndPolicyNameContainingIgnoreCaseAndStatus(userId, query, filter, pageable);
        }

        return new PolicyPage(
                policyPage.getContent().stream().map(policy ->
                        new PolicyListItemDto(
                                policy.getId(),
                                policy.getPolicyName(),
                                policyFileRepository.countByPolicy_Id(policy.getId()),
                                policyRecipientRepository.countByPolicy_Id(policy.getId()),
                                policy.getStatus(),
                                null,
                                policy.getType(),
                                policy.getCreatedAt()
                        )
                ).toList(),
                policyPage.getTotalElements()
        );
    }

    public PolicyDetailsDto getPolicy(UUID userId, UUID policyId) {
        Policy policy = conditionalReleaseRepository
                .findByIdAndOwner_Id(policyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojo politika nerasta!"));

        return new PolicyDetailsDto(
                policy.getId(),
                policy.getPolicyName(),
                policy.getType(),
                policy.getWarningChannel(),
                policy.getInactivityDays(),
                policy.getGraceDays(),
                policy.getWarningEveryHours(),
                policy.getScheduledReleaseAt(),
                policy.getWarnBeforeDays(),
                policy.getCreatedAt(),
                policy.getStatus(),
                policy.getType(),
                null,
                policyFileRepository.findAllByPolicy_Id(policy.getId())
                        .stream()
                        .map(policyFile -> new FileMetaSummary(
                                policyFile.getFile().getId(),
                                policyFile.getFile().getStorageType(),
                                policyFile.getFile().getCreatedAt()
                        )).toList(),
                policyRecipientRepository.findAllByPolicy_Id(policy.getId())
                        .stream()
                        .map(policyRecipient -> new PolicyRecipientDto(
                                policyRecipient.getRecipient().getId(),
                                policyRecipient.getRecipient().getEmail(),
                                policyRecipient.getAccessCount(),
                                policyRecipient.getFirstAccessAt(),
                                policyRecipient.getLastAccessAt()
                        )).toList()
        );
    }

    public void deleteUserPolicy(UUID userId, UUID policyId) {
        try {
            Policy policy = conditionalReleaseRepository.findByIdAndOwner_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojo politika nerasta."));
            conditionalReleaseRepository.delete(policy);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.POLICY_DELETE,
                    "Sąlyginio atskleidimo politika sėkmingai ištrinta",
                    Map.of(
                            "policyId", policy.getId(),
                            "policyName", policy.getPolicyName(),
                            "policyType", policy.getType()
                    )
            );
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_DELETE,
                    "Sąlyginio atskleidimo politikos ištrinti nepavyko",
                    Map.of(
                            "policyId", policyId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    @Transactional
    public void updateUserPolicy(UUID userId, UUID policyId, NewPolicy newPolicy) {
        try {
            Policy existingPolicy = conditionalReleaseRepository.findByIdAndOwner_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojo politika nerasta!"));

            if (existingPolicy.getStatus() == PolicyStatus.RELEASED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atskleistos politikos redaguoti negalima!");
            }

            PolicyDto newPolicy_ = newPolicy.policy();
            ResolvedPolicyData resolved = resolvePolicyData(newPolicy);
            updatePolicyFields(existingPolicy, newPolicy_);

            Policy savedPolicy = conditionalReleaseRepository.save(existingPolicy);

            policyRecipientRepository.deleteAllByPolicy_Id(savedPolicy.getId());
            policyFileRepository.deleteAllByPolicy_Id(savedPolicy.getId());

            savePolicyRelations(savedPolicy, newPolicy, resolved);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.POLICY_UPDATE,
                    "Sąlyginio atskleidimo politika atnaujinta sėkmingai!",
                    Map.of(
                            "policyId", savedPolicy.getId(),
                            "policyName", savedPolicy.getPolicyName(),
                            "policyType", savedPolicy.getType()
                    )
            );
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_UPDATE,
                    "Nepavyko atnaujinti sąlyginio atskleidimo politikos",
                    Map.of(
                            "policyId", policyId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    /**
     * Here we're validating the new policy data:
     * <br>
     * <ul>
     * <li>Removing duplicates.</li>
     * <li>Retrieving files/recipients from the DB and checking if the key/access data matches the selected files and recipients.</li>
     * </ul>
    */
    private ResolvedPolicyData resolvePolicyData(NewPolicy newPolicy) {
        if (newPolicy == null || newPolicy.policy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Būtina pateikti politikos duomenis!");
        }

        if (newPolicy.policy().files() == null || newPolicy.policy().recipients() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Būtina pasirinkti failus ir gavėjus!");
        }

        // Removing null values and duplicates
        List<UUID> fileIds = newPolicy.policy().files().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> recipientIds = newPolicy.policy().recipients().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (fileIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Būtina pasirinkti bent vieną failą!");
        }

        if (recipientIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Būtina pasirinkti bent vieną gavėją!");
        }

        List<File> files = fileStorageRepository.findAllById(fileIds);
        List<User> recipients = userRepository.findAllById(recipientIds);

        if (files.size() != fileIds.size()) {
            throw new IllegalArgumentException("Kai kurie failai nerasti!");
        }

        if (recipients.size() != recipientIds.size()) {
            throw new IllegalArgumentException("Kai kurie gavėjai nerasti!");
        }

        if (newPolicy.recipientAccesses() == null || newPolicy.recipientAccesses().size() != recipientIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gavėjų prieigos duomenys neatitinka gavėjų duomenų!");
        }

        if (newPolicy.encryptedFileKeys() == null || newPolicy.encryptedFileKeys().size() != fileIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Užšifruoti failų raktai neatitinka failų!");
        }

        return new ResolvedPolicyData(files, recipients);
    }

    private void updatePolicyFields(Policy policy, PolicyDto newPolicy) {
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setPolicyName(newPolicy.policyName());
        policy.setType(newPolicy.releaseType());
        policy.setLastWarningSentAt(null);
        policy.setGraceStartedAt(null);
        policy.setWarningChannel(newPolicy.channel());
        policy.setUpdatedAt(OffsetDateTime.now());

        switch (newPolicy.releaseType()){
            case INACTIVITY -> {
                inactivityPolicyCheck(newPolicy);
                policy.setInactivityDays(newPolicy.inactivityDays());
                policy.setGraceDays(newPolicy.graceDays());

                if (policy.getLastHeartbeatAt() == null) {
                    policy.setLastHeartbeatAt(OffsetDateTime.now());
                }

                policy.setScheduledReleaseAt(null);
                policy.setWarningEveryHours(newPolicy.warningEveryHours());
                policy.setWarnBeforeDays(null);
            }
            case DATE_TIME -> {
                dateTimePolicyCheck(newPolicy);
                policy.setInactivityDays(null);
                policy.setGraceDays(null);
                policy.setScheduledReleaseAt(newPolicy.scheduledReleaseAt());
                policy.setWarningEveryHours(null);
                policy.setWarnBeforeDays(newPolicy.warningBeforeDays());
                policy.setLastHeartbeatAt(null);
            }
            case MANUAL_RELEASE -> {
                policy.setInactivityDays(null);
                policy.setGraceDays(null);
                policy.setScheduledReleaseAt(null);
                policy.setWarningEveryHours(null);
                policy.setWarnBeforeDays(null);
                policy.setLastHeartbeatAt(null);
            }
        }
    }

    /**
     * Saving data in to the separated many-to-many tables (policyRecipients, policyFiles)
    * */
    private void savePolicyRelations(Policy savedPolicy, NewPolicy newPolicy, ResolvedPolicyData resolved){
        for (RecipientPolicyAccess recipientAccess : newPolicy.recipientAccesses()) {
            policyRecipientRepository.save(
                    new PolicyRecipient(
                            new PolicyRecipientId(savedPolicy.getId(), recipientAccess.recipientUserId()),
                            savedPolicy,
                            resolved.recipients().stream()
                                    .filter(r -> r.getId().equals(recipientAccess.recipientUserId()))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Gavėjas nerastas!")),
                            Base64.getDecoder().decode(recipientAccess.encryptedPolicyKeyForRecipientB64()),
                            recipientAccess.manifest(),
                            Base64.getDecoder().decode(recipientAccess.signatureByOwnerB64()),
                            OffsetDateTime.now()
                    )
            );
        }

        for (EncryptedPolicyFileKey policyFileKey : newPolicy.encryptedFileKeys()) {
            policyFileRepository.save(
                    new PolicyFile(
                            new PolicyFileId(savedPolicy.getId(), policyFileKey.fileId()),
                            savedPolicy,
                            resolved.files().stream()
                                    .filter(f -> f.getId().equals(policyFileKey.fileId()))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Failas nerastas!")),
                            Base64.getDecoder().decode(policyFileKey.fileKeyEncryptedB64()),
                            Base64.getDecoder().decode(policyFileKey.fileKeyNonceB64()),
                            Base64.getDecoder().decode(policyFileKey.metaKeyEncryptedB64()),
                            Base64.getDecoder().decode(policyFileKey.metaKeyNonceB64()),
                            OffsetDateTime.now()
                    )
            );
        }
    }

    public void processPendingPolicies() {
        List<Policy> policies = conditionalReleaseRepository.findAllByStatusIn(
                List.of(PolicyStatus.ACTIVE, PolicyStatus.IN_GRACE)
        );

        for (Policy policy : policies){
            switch (policy.getType()){
                case DATE_TIME -> processDateTimePolicy(policy);
                case INACTIVITY -> processInactivityPolicy(policy);
                case MANUAL_RELEASE -> {}
            }
        }
    }

    private void processDateTimePolicy(Policy policy) {
        if (policy.getScheduledReleaseAt() == null) return;
        if (policy.getStatus() == PolicyStatus.RELEASED) return;

        OffsetDateTime now = OffsetDateTime.now();

        // now >= targetTime
        if (!now.isBefore(policy.getScheduledReleaseAt())) {
            releasePolicy(policy, ReleaseTrigger.SCHEDULER);
            return;
        }

        processDateTimeWarning(policy, now);
    }

    private void processDateTimeWarning(Policy policy, OffsetDateTime now){
        if (policy.getWarnBeforeDays() == null || policy.getWarnBeforeDays() <= 0) return;
        if (policy.getLastWarningSentAt() != null) return; // If warning has already been sent – do not send again.

        OffsetDateTime warningTime = policy.getScheduledReleaseAt().minusDays(policy.getWarnBeforeDays());

        if (!now.isBefore(warningTime)){
            policy.setLastWarningSentAt(now);
            policy.setUpdatedAt(now);
            conditionalReleaseRepository.save(policy);

            policyNotificationService.sendDateTimeWarning(policy);

            auditLogService.log(
                    policy.getOwner().getId(),
                    AuditLevel.INFO,
                    AuditAction.POLICY_WARNING_SEND,
                    "Suplanuoto atskleidimo politikos įspėjimas apdorotas scheduler",
                    Map.of(
                            "policyId", policy.getId(),
                            "policyName", policy.getPolicyName(),
                            "policyType", policy.getType(),
                            "trigger", ReleaseTrigger.SCHEDULER.name()
                    )
            );
        }
    }

    private void processInactivityPolicy(Policy policy) {
        if (policy.getInactivityDays() == null) return;
        if (policy.getLastHeartbeatAt() == null) return; // Without the last active time it is impossible to calculate inactivity.
        if (policy.getStatus() == PolicyStatus.RELEASED) return;

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime inactivityDeadline = policy.getLastHeartbeatAt().plusDays(policy.getInactivityDays());

        if (policy.getStatus() == PolicyStatus.ACTIVE && !now.isBefore(inactivityDeadline)){
            startGraceOrRelease(policy, now);
            return;
        }

        if (policy.getStatus() == PolicyStatus.IN_GRACE){
            processGracePeriod(policy, now);
        }

    }

    private void startGraceOrRelease(Policy policy, OffsetDateTime now) {
        Integer graceDays = policy.getGraceDays();

        if (graceDays != null && graceDays > 0){
            OffsetDateTime graceDeadline = now.plusDays(graceDays);
            policy.setStatus(PolicyStatus.IN_GRACE);
            policy.setGraceStartedAt(now);
            policy.setUpdatedAt(now);
            conditionalReleaseRepository.save(policy);

            policyNotificationService.sendGraceStarted(policy, graceDeadline);

            auditLogService.log(
                    policy.getOwner().getId(),
                    AuditLevel.INFO,
                    AuditAction.POLICY_GRACE_STARTED,
                    "Politika perėjo į atidėjimo laikotarpį",
                    Map.of(
                            "policyId", policy.getId(),
                            "policyName", policy.getPolicyName(),
                            "policyType", policy.getType(),
                            "graceStartedAt", now,
                            "graceDeadline", graceDeadline,
                            "trigger", ReleaseTrigger.SCHEDULER.name()
                    )
            );
        }
        else {
            releasePolicy(policy, ReleaseTrigger.SCHEDULER);
        }
    }

    private void processGracePeriod(Policy policy, OffsetDateTime now) {
        if (policy.getGraceStartedAt() == null) return;
        if (policy.getGraceDays() == null) return;

        OffsetDateTime graceDeadline = policy.getGraceStartedAt().plusDays(policy.getGraceDays());

        if (!now.isBefore(graceDeadline)) {
            releasePolicy(policy, ReleaseTrigger.SCHEDULER);
            return;
        }

        processGraceWarning(policy, now, graceDeadline);
    }

    private void processGraceWarning(Policy policy, OffsetDateTime now, OffsetDateTime graceDeadline) {
        if (policy.getWarningEveryHours() == null || policy.getWarningEveryHours() <= 0) return;

        if (policy.getLastWarningSentAt() == null) {
            policy.setLastWarningSentAt(now);
            policy.setUpdatedAt(now);
            conditionalReleaseRepository.save(policy);

            policyNotificationService.sendGraceWarning(policy, graceDeadline);
            return;
        }

        OffsetDateTime nextWarningTime = policy.getLastWarningSentAt().plusHours(policy.getWarningEveryHours());

        if (!now.isBefore(nextWarningTime)) {
            policy.setLastWarningSentAt(now);
            policy.setUpdatedAt(now);
            conditionalReleaseRepository.save(policy);

            policyNotificationService.sendGraceWarning(policy, graceDeadline);
        }
    }

    private void releasePolicy(Policy policy, ReleaseTrigger trigger) {
        try {
            if (policy.getStatus() == PolicyStatus.RELEASED) return;

            OffsetDateTime now = OffsetDateTime.now();

            policy.setStatus(PolicyStatus.RELEASED);
            policy.setReleasedAt(now);
            policy.setUpdatedAt(now);
            conditionalReleaseRepository.save(policy);

            policyNotificationService.sendReleased(policy);

            auditLogService.log(
                    policy.getOwner().getId(),
                    AuditLevel.INFO,
                    AuditAction.POLICY_RELEASE,
                    "Politika atskleista",
                    Map.of(
                            "policyId", policy.getId(),
                            "policyName", policy.getPolicyName(),
                            "policyType", policy.getType(),
                            "trigger", trigger.name()
                    )
            );
        }
        catch (Exception e){
            auditLogService.log(
                    policy.getOwner().getId(),
                    AuditLevel.ALERT,
                    AuditAction.POLICY_RELEASE,
                    "Nepavyko atskleisti politikos",
                    Map.of(
                            "policyId", policy.getId(),
                            "trigger", trigger.name(),
                            "reason", e.getClass().getSimpleName()
                    )
            );
            /*
            * Rethrowing the exception, because Spring will rollback the transaction.
            * If we swallow it, Spring may commit partial changes.
            */
            throw e;
        }
    }

    /**
     * Releasing the selected policy manually when requested by its owner.
     */
    public void releasePolicyNow(UUID userId, UUID policyId){
        try {
            Policy policy = conditionalReleaseRepository.findByIdAndOwner_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savininko politika nerasta!"));

            if (policy.getStatus() == PolicyStatus.RELEASED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Politika jau atskleista!");
            }

            releasePolicy(policy, ReleaseTrigger.USER_REQUEST);
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_RELEASE,
                    "Nepavyko atskleisti politikos",
                    Map.of(
                            "policyId", policyId,
                            "trigger", ReleaseTrigger.USER_REQUEST.name(),
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    public List<RecipientValidationItem> validateRecipients(List<UUID> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return List.of();
        }

        // Removing null values and duplicates
        List<UUID> uniqueIds = recipientIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        List<User> recipients = userRepository.findAllById(uniqueIds);

        if (recipients.size() != uniqueIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vienas ar daugiau gavėjų nerasti!");
        }

        return uniqueIds.stream().map(rId -> new RecipientValidationItem(
                rId,
                userTelegramLinkRepository.existsById(rId),
                userKeyRepository.existsByUserId(rId)
        )).toList();

    }

    public void refreshHeartbeatForUser(UUID userId) {
        List<Policy> policies = conditionalReleaseRepository
                .findAllByOwner_IdAndTypeAndStatusIn(
                        userId,
                        PolicyType.INACTIVITY,
                        List.of(PolicyStatus.ACTIVE, PolicyStatus.IN_GRACE)
                );

        OffsetDateTime now = OffsetDateTime.now();

        policies.forEach(p -> {
            p.setLastHeartbeatAt(now);
            p.setStatus(PolicyStatus.ACTIVE);
            p.setGraceStartedAt(null);
            p.setLastWarningSentAt(null);
            p.setUpdatedAt(now);
        });

        conditionalReleaseRepository.saveAll(policies);
    }

    public void pausePolicy(UUID userId, UUID policyId){
        try {
            Policy policy = conditionalReleaseRepository.findByIdAndOwner_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojui priklausanti politika nerasta!"));

            if (policy.getStatus() == PolicyStatus.RELEASED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atskleistos politikos negalima pristabdyti!");
            }

            if (policy.getStatus() == PolicyStatus.PAUSED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Politika jau yra pristabdyta!");
            }

            OffsetDateTime now = OffsetDateTime.now();

            policy.setStatus(PolicyStatus.PAUSED);
            policy.setGraceStartedAt(null);
            policy.setLastWarningSentAt(null);
            policy.setUpdatedAt(now);

            conditionalReleaseRepository.save(policy);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.POLICY_PAUSE,
                    "Sąlyginio atskleidimo politika sėkmingai pristabdyta",
                    Map.of(
                            "policyId", policy.getId(),
                            "policyName", policy.getPolicyName(),
                            "policyType", policy.getType()
                    )
            );
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_PAUSE,
                    "Nepavyko pristabdyti sąlyginio atskleidimo politikos",
                    Map.of(
                            "policyId", policyId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    public void resumePolicy(UUID userId, UUID policyId){
        try {
            Policy policy = conditionalReleaseRepository.findByIdAndOwner_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Politika nerasta arba ji nepriklauso naudotojui!"));

            if (policy.getStatus() == PolicyStatus.RELEASED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atskleistos politikos negalima atnaujinti!");
            }

            if (policy.getStatus() != PolicyStatus.PAUSED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tęsti galima tik pristabdytą politiką!");
            }

            OffsetDateTime now = OffsetDateTime.now();

            // Resume must also guarantee a clean state / reset
            policy.setStatus(PolicyStatus.ACTIVE);
            policy.setGraceStartedAt(null);
            policy.setLastWarningSentAt(null);
            policy.setUpdatedAt(now);

            // Needed because policy can go in grace mode after pause
            if (policy.getType() == PolicyType.INACTIVITY) {
                policy.setLastHeartbeatAt(now);
            }

            conditionalReleaseRepository.save(policy);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.POLICY_RESUME,
                    "Sąlyginio atskleidimo politika sėkmingai tęsiama",
                    Map.of(
                            "policyId", policy.getId(),
                            "policyName", policy.getPolicyName(),
                            "policyType", policy.getType()
                    )
            );
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.POLICY_RESUME,
                    "Nepavyko tęsti sąlyginio atskleidimo politikos",
                    Map.of(
                            "policyId", policyId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    private record ResolvedPolicyData(List<File> files, List<User> recipients){}
}