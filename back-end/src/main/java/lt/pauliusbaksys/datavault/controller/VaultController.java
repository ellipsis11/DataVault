package lt.pauliusbaksys.datavault.controller;
import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.service.UserKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vault")
public class VaultController {

    private final UserKeyService userKeyService;
    private final CurrentUserService currentUserService;
    private final Logger logger = LoggerFactory.getLogger(VaultController.class);

    public VaultController(UserKeyService userKeyService, CurrentUserService currentUserService) {
        this.userKeyService = userKeyService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCurrentUserKeys(@RequestBody UserKeysFull userKeysFull, HttpServletRequest req){
        userKeyService.createUserKeys(currentUserService.getCurrentUserId(req), userKeysFull);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public UserKeysFull getCurrentUserKeysFull(HttpServletRequest req){
        return userKeyService.getUserKeysFull(currentUserService.getCurrentUserId(req));
    }

    @GetMapping("/basic")
    public UserKeysBasic getCurrentUserKeys(HttpServletRequest req){
        return userKeyService.getUserKeys(currentUserService.getCurrentUserId(req));
    }

    @GetMapping("/status")
    public ResponseEntity<?> checkIfVaultRecordExitsForCurrentUser(HttpServletRequest req){
        boolean exists = userKeyService.checkForUserKeys(currentUserService.getCurrentUserId(req));
        return exists ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/recipient-public-keys")
    public List<RecipientPublicKey> getRecipientPublicKeys(@RequestBody List<UUID> recipientIds){
        return userKeyService.getRecipientPublicKeys(recipientIds);
    }

    @GetMapping("/owner-sign-private-key")
    public UserSignPrivateKey getOwnerSignPrivateKey(HttpServletRequest req){
        return userKeyService.getOwnerSignPrivateKey(currentUserService.getCurrentUserId(req));
    }

    @GetMapping("/recipient-unlock")
    public RecipientUnlockKeys getRecipientUnlockKeys(HttpServletRequest req) {
        return userKeyService.getRecipientUnlockKeys(currentUserService.getCurrentUserId(req));
    }
}
