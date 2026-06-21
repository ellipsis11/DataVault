package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.*;
import lt.pauliusbaksys.datavault.model.*;
import lt.pauliusbaksys.datavault.repository.PolicyFileRepository;
import lt.pauliusbaksys.datavault.repository.PolicyRecipientRepository;
import lt.pauliusbaksys.datavault.repository.UserKeyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReceivedPoliciesService {

    private final PolicyRecipientRepository policyRecipientRepository;
    private final PolicyFileRepository policyFileRepository;
    private final ObjectStorageResolver objectStorageResolver;
    private final AuditLogService auditLogService;
    private final UserKeyRepository userKeyRepository;

    public ReceivedPoliciesService(PolicyRecipientRepository policyRecipientRepository, PolicyFileRepository policyFileRepository, ObjectStorageResolver objectStorageResolver, AuditLogService auditLogService, UserKeyRepository userKeyRepository) {
        this.policyRecipientRepository = policyRecipientRepository;
        this.policyFileRepository = policyFileRepository;
        this.objectStorageResolver = objectStorageResolver;
        this.auditLogService = auditLogService;
        this.userKeyRepository = userKeyRepository;
    }

    public ReceivedPoliciesCount getReceivedPoliciesCounts(UUID userId) {
        List<PolicyRecipient> policyRecipients = policyRecipientRepository.findAllByRecipient_Id(userId).stream()
                .filter(pr -> pr.getPolicy().getStatus() == PolicyStatus.RELEASED)
                .toList();

        if (policyRecipients.isEmpty()) {
            return new ReceivedPoliciesCount(0,0);
        }

        int newCount = (int) policyRecipients.stream()
                .filter(pr -> pr.getFirstAccessAt() == null)
                .count();

        return new ReceivedPoliciesCount(policyRecipients.size(), newCount);
    }

    public ReceivedPolicyPage getReceivedPolicyPage(UUID userId, String query, ReceivedPolicyFilter filter, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "policy.releasedAt"));

        Boolean viewed = null;

        if (filter == ReceivedPolicyFilter.NEW){
            viewed = false;
        }
        else if (filter == ReceivedPolicyFilter.VIEWED){
            viewed = true;
        }

        Page<PolicyRecipient> policyRecipients = policyRecipientRepository.searchViewedReceivedPolicies(
                userId,
                PolicyStatus.RELEASED,
                query,
                viewed,
                pageable
        );

        List<ReceivedPolicyListItemDto> policyList = policyRecipients.getContent().stream().map(pr ->{
            Policy policy = pr.getPolicy();
            List<PolicyFile> policyFiles = policyFileRepository.findAllByPolicy_Id(policy.getId());
            return new ReceivedPolicyListItemDto(
                    policy.getId(),
                    policy.getPolicyName(),
                    policy.getOwner().getEmail(),
                    policyFiles.size(),
                    policy.getReleasedAt(),
                    pr.getFirstAccessAt() != null
            );
        }).toList();

        return new ReceivedPolicyPage(
                policyList,
                policyRecipients.getTotalElements()
        );
    }

    public ReceivedPolicyDetailsDto getReceivedPolicyDetails(UUID userId, UUID policyId) {
        try {
            PolicyRecipient policyRecipient = policyRecipientRepository.findByPolicy_IdAndRecipient_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gavėjui skirta politika nerasta!"));

            Policy policy = policyRecipient.getPolicy();

            if (policy.getStatus() != PolicyStatus.RELEASED){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Politika dar neatskleista!");
            }

            User owner = policy.getOwner();
            List<PolicyFile> policyFiles = policyFileRepository.findAllByPolicy_Id(policy.getId());

            OffsetDateTime now = OffsetDateTime.now();

            policyRecipient.setAccessCount(policyRecipient.getAccessCount() + 1);

            if (policyRecipient.getFirstAccessAt() == null) {
                policyRecipient.setFirstAccessAt(now);
            }

            policyRecipient.setLastAccessAt(now);
            policyRecipientRepository.save(policyRecipient);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.RECEIVED_POLICY_ACCESS,
                    "Gavėjas atidarė jam skirtą politiką",
                    Map.of(
                            "policyId", policyId,
                            "recipientUserId", userId
                    )
            );

            return new ReceivedPolicyDetailsDto(
                    policy.getId(),
                    policy.getPolicyName(),
                    owner.getEmail(),
                    policy.getType(),
                    policy.getReleasedAt(),
                    policyRecipient.getFirstAccessAt() != null,
                    policyRecipient.getFirstAccessAt(),
                    policyRecipient.getAccessCount(),
                    policyRecipient.getLastAccessAt(),
                    policyFiles.stream().map(policyFile ->
                            new FileMetaSummary(
                                    policyFile.getFile().getId(),
                                    policyFile.getFile().getStorageType(),
                                    policyFile.getFile().getCreatedAt()
                            )
                    ).toList()
            );
        }
        catch (ResponseStatusException e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.RECEIVED_POLICY_ACCESS,
                    "Nepavyko pasiekti gavėjui skirtos politikos",
                    Map.of(
                            "policyId", policyId,
                            "recipientUserId", userId,
                            "reason", e.getStatusCode().toString()
                    )
            );
            throw e;
        }
    }

    public ReceivedFileMetaAccessDto getReceivedFileMetaAccess(UUID userId, UUID policyId, UUID fileId, MetaAccessPurpose purpose) {
        try {
            PolicyRecipient policyRecipient = policyRecipientRepository.findByPolicy_IdAndRecipient_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gavėjui skirta politika nerasta!"));

            if (policyRecipient.getPolicy().getStatus() != PolicyStatus.RELEASED){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Politika dar neatskleista!");
            }

            PolicyFile policyFile = policyFileRepository.findByPolicy_IdAndFile_Id(policyId, fileId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failas nerastas šioje politikoje!"));

            File file = policyFile.getFile();

            if (policyRecipient.getManifest() == null || policyRecipient.getSignatureByOwner() == null) {
                // INTERNAL_SERVER_ERROR for indicating an invalid or corrupted system state, because manifest was not created
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Politikos manifestas arba parašas nerastas!");
            }

            UserKey ownerKeys = userKeyRepository.findUserKeyByUserId(policyRecipient.getPolicy().getOwner().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savininko raktai nerasti!"));

            if (purpose == MetaAccessPurpose.VIEW){
                auditLogService.log(
                        userId,
                        AuditLevel.INFO,
                        AuditAction.RECEIVED_POLICY_ACCESS,
                        "Gavėjas gavo prieigą prie atskleisto failo metaduomenų",
                        Map.of(
                                "policyId", policyId,
                                "fileId", fileId,
                                "recipientUserId", userId,
                                "ownerUserId", file.getOwner().getId()
                        )
                );
            }

            return new ReceivedFileMetaAccessDto(
                    policyId,
                    fileId,
                    file.getOwner().getId(),
                    Base64.getEncoder().encodeToString(policyRecipient.getEncryptedPolicyKeyForRecipient()),
                    Base64.getEncoder().encodeToString(policyFile.getEncryptedMetaKeyByPolicy()),
                    Base64.getEncoder().encodeToString(policyFile.getEncryptedMetaKeyNonce()),
                    Base64.getEncoder().encodeToString(file.getMetaCipher()),
                    Base64.getEncoder().encodeToString(file.getMetaNonce()),
                    policyRecipient.getManifest(),
                    Base64.getEncoder().encodeToString(policyRecipient.getSignatureByOwner()),
                    Base64.getEncoder().encodeToString(ownerKeys.getOwnerSignPublicKey())
            );
        }
        catch (ResponseStatusException e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.RECEIVED_POLICY_ACCESS,
                    "Nepavyko gauti atskleisto failo metaduomenų",
                    Map.of(
                            "policyId", policyId,
                            "fileId", fileId,
                            "recipientUserId", userId,
                            "reason", e.getStatusCode().toString()
                    )
            );
            throw e;
        }
    }

    public ResponseEntity<byte[]> downloadReceivedEncryptedFile(UUID userId, UUID policyId, UUID fileId) {
        try {
            Policy policy = policyRecipientRepository.findByPolicy_IdAndRecipient_Id(policyId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gavėjui skirta politika nerasta!"))
                    .getPolicy();

            if (policy.getStatus() != PolicyStatus.RELEASED){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Politika dar neatskleista!");
            }

            PolicyFile policyFile = policyFileRepository.findByPolicy_IdAndFile_Id(policyId, fileId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failas nerastas šioje politikoje!"));

            File file = policyFile.getFile();

            String objectKey = file.getStorageRef();
            ObjectStorageService storageService = objectStorageResolver.resolve(file.getStorageType());

            byte[] encryptedFile = storageService.download(objectKey);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.RECEIVED_FILE_DOWNLOAD,
                    "Gavėjas sėkmingai atsisiuntė gautą failą",
                    Map.of(
                            "policyId", policyId,
                            "fileId", fileId,
                            "recipientUserId", userId
                    )
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(encryptedFile.length)
                    .body(encryptedFile);

        } catch (ResponseStatusException e) {
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.RECEIVED_FILE_DOWNLOAD,
                    "Nepavyko atsisiųsti atskleisto failo",
                    Map.of(
                            "policyId", policyId,
                            "fileId", fileId,
                            "recipientUserId", userId,
                            "reason", e.getStatusCode().toString()
                    )
            );
            throw e;
        }
        catch (RuntimeException e) {
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.RECEIVED_FILE_DOWNLOAD,
                    "Nepavyko atsisiųsti atskleisto failo",
                    Map.of(
                            "policyId", policyId,
                            "fileId", fileId,
                            "recipientUserId", userId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nepavyko atsisiųsti failo!", e);
        }
    }

    public ReceivedFileContentAccessDto getReceivedFileContentAccess(UUID userId, UUID policyId, UUID fileId) {
        PolicyRecipient policyRecipient = policyRecipientRepository.findByPolicy_IdAndRecipient_Id(policyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gavėjui skirta politika nerasta!"));

        if (policyRecipient.getPolicy().getStatus() != PolicyStatus.RELEASED){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Politika dar neatskleista!");
        }

        PolicyFile policyFile = policyFileRepository.findByPolicy_IdAndFile_Id(policyId, fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failas nerastas politikoje!"));

        File file = policyFile.getFile();

        if (policyRecipient.getManifest() == null || policyRecipient.getSignatureByOwner() == null) {
            // INTERNAL_SERVER_ERROR for indicating an invalid or corrupted system state, because manifest was not created
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Politikos manifestas arba parašas nerastas!");
        }

        UserKey ownerKeys = userKeyRepository.findUserKeyByUserId(policyRecipient.getPolicy().getOwner().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savininko raktai nerasti!"));

        return new ReceivedFileContentAccessDto(
                policyId,
                fileId,
                file.getOwner().getId(),
                Base64.getEncoder().encodeToString(policyRecipient.getEncryptedPolicyKeyForRecipient()),
                Base64.getEncoder().encodeToString(policyFile.getEncryptedFileKeyByPolicy()),
                Base64.getEncoder().encodeToString(policyFile.getEncryptedFileKeyNonce()),
                policyRecipient.getManifest(),
                Base64.getEncoder().encodeToString(policyRecipient.getSignatureByOwner()),
                Base64.getEncoder().encodeToString(ownerKeys.getOwnerSignPublicKey())
        );
    }

    public ResponseEntity<?> deleteReceivedPolicyAccess(UUID userId, UUID policyId) {
        PolicyRecipient policyRecipient = policyRecipientRepository.findByPolicy_IdAndRecipient_Id(policyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gavėjui skirta politika nerasta!"));

        if (policyRecipient.getPolicy().getStatus() != PolicyStatus.RELEASED){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Politika dar neatskleista!");
        }

        policyRecipientRepository.delete(policyRecipient);

        return ResponseEntity.noContent().build();
    }
}
