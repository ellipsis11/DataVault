package lt.pauliusbaksys.datavault.controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.service.ConditionalReleaseService;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

//Check the API naming!!!
@RestController
@RequestMapping("/api/conditional-release")
public class ConditionalReleaseController {

    private final ConditionalReleaseService conditionalReleaseService;
    private final CurrentUserService currentUserService;
    private final Logger logger = LoggerFactory.getLogger(ConditionalReleaseController.class);


    public ConditionalReleaseController(ConditionalReleaseService conditionalReleaseService, CurrentUserService currentUserService) {
        this.conditionalReleaseService = conditionalReleaseService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPolicy(HttpServletRequest req, @Valid @RequestBody NewPolicy newPolicy){
        conditionalReleaseService.createNewConditionalReleasePolicy(currentUserService.getCurrentUserId(req), newPolicy);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/policies")
    public PolicyPage getPolicies(
            HttpServletRequest req,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) PolicyStatus filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return conditionalReleaseService.getPoliciesPage(currentUserService.getCurrentUserId(req), query.trim(), filter, page, size);
    }

    @GetMapping("/policy/{policyId}")
    public PolicyDetailsDto getPolicy(HttpServletRequest req, @PathVariable UUID policyId){
        return conditionalReleaseService.getPolicy(currentUserService.getCurrentUserId(req), policyId);
    }

    @PutMapping("/policy/{policyId}")
    public ResponseEntity<?> updatePolicy(HttpServletRequest req, @PathVariable UUID policyId, @RequestBody NewPolicy newPolicy){
        conditionalReleaseService.updateUserPolicy(currentUserService.getCurrentUserId(req), policyId, newPolicy);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/policy/{policyId}")
    public ResponseEntity<?> deletePolicy(HttpServletRequest req, @PathVariable UUID policyId){
        conditionalReleaseService.deleteUserPolicy(currentUserService.getCurrentUserId(req), policyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/policy/{policyId}/release")
    public ResponseEntity<?> releasePolicyNow(HttpServletRequest req, @PathVariable UUID policyId) {
        conditionalReleaseService.releasePolicyNow(currentUserService.getCurrentUserId(req), policyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-recipients")
    public List<RecipientValidationItem> validateRecipients(@RequestBody List<UUID> recipientIds){
        return conditionalReleaseService.validateRecipients(recipientIds);
    }

    @PostMapping("/policy/{policyId}/pause")
    public ResponseEntity<?> pausePolicy(HttpServletRequest req, @PathVariable UUID policyId){
        conditionalReleaseService.pausePolicy(currentUserService.getCurrentUserId(req), policyId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/policy/{policyId}/resume")
    public ResponseEntity<?> resumePolicy(HttpServletRequest req, @PathVariable UUID policyId){
        conditionalReleaseService.resumePolicy(currentUserService.getCurrentUserId(req), policyId);
        return ResponseEntity.ok().build();
    }
}
