package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.MetaAccessPurpose;
import lt.pauliusbaksys.datavault.enums.ReceivedPolicyFilter;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.service.ReceivedPoliciesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/received-policies")
public class ReceivedPoliciesController {

    private final ReceivedPoliciesService receivedPoliciesService;
    private final CurrentUserService currentUserService;

    public ReceivedPoliciesController(ReceivedPoliciesService receivedPoliciesService, CurrentUserService currentUserService) {
        this.receivedPoliciesService = receivedPoliciesService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/counts")
    public ReceivedPoliciesCount getReceivedPoliciesCounts(HttpServletRequest req){
        return receivedPoliciesService.getReceivedPoliciesCounts(currentUserService.getCurrentUserId(req));
    }

    @GetMapping
    public ReceivedPolicyPage getReceivedPolicies(
            HttpServletRequest req,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) ReceivedPolicyFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return receivedPoliciesService.getReceivedPolicyPage(currentUserService.getCurrentUserId(req), query.trim(), filter, page, size);
    }

    @GetMapping("/{policyId}")
    public ReceivedPolicyDetailsDto getReceivedPolicyDetails(HttpServletRequest req, @PathVariable UUID policyId) {
        return receivedPoliciesService.getReceivedPolicyDetails(currentUserService.getCurrentUserId(req), policyId);
    }

    @GetMapping("/{policyId}/file-meta/{fileId}")
    public ReceivedFileMetaAccessDto getReceivedFileMetaAccess(
            HttpServletRequest req,
            @PathVariable UUID policyId,
            @PathVariable UUID fileId,
            @RequestParam(defaultValue = "VIEW") MetaAccessPurpose purpose
    ){
        return receivedPoliciesService.getReceivedFileMetaAccess(currentUserService.getCurrentUserId(req), policyId, fileId, purpose);
    }

    @GetMapping("/{policyId}/file-content/{fileId}")
    public ReceivedFileContentAccessDto getReceivedFileContentAccess(HttpServletRequest req, @PathVariable UUID policyId, @PathVariable UUID fileId
    ) {
        return receivedPoliciesService.getReceivedFileContentAccess(
                currentUserService.getCurrentUserId(req),
                policyId,
                fileId
        );
    }

    @GetMapping("/{policyId}/file/{fileId}/download")
    public ResponseEntity<byte[]> downloadReceivedEncryptedFile(HttpServletRequest req, @PathVariable UUID policyId, @PathVariable UUID fileId){
        return receivedPoliciesService.downloadReceivedEncryptedFile(
            currentUserService.getCurrentUserId(req),
            policyId,
            fileId
        );
    }

    @DeleteMapping("/{policyId}/delete")
    public ResponseEntity<?> deleteReceivedPolicyAccess(HttpServletRequest req, @PathVariable UUID policyId){
        return receivedPoliciesService.deleteReceivedPolicyAccess(currentUserService.getCurrentUserId(req), policyId);
    }
}
