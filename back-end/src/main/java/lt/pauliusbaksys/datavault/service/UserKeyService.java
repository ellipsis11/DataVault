package lt.pauliusbaksys.datavault.service;
import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.model.UserKey;
import lt.pauliusbaksys.datavault.repository.UserKeyRepository;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserKeyService {

    private final UserRepository userRepository;
    private final UserKeyRepository userKeyRepository;
    private final AuditLogService auditLogService;

    public UserKeyService(UserRepository userRepository, UserKeyRepository userKeyRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.userKeyRepository = userKeyRepository;
        this.auditLogService = auditLogService;
    }

    public void createUserKeys(UUID userId, UserKeysFull userKeysFull){
        try {
            User user = userRepository.findById(userId)
                   .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND ,"Naudotojas nerastas: " + userId));

                byte[] wrappedMasterKey = Base64.getDecoder().decode(userKeysFull.wrappedMasterKeyB64());
                byte[] salt = Base64.getDecoder().decode(userKeysFull.kdfSaltB64());
                byte[] nonce = Base64.getDecoder().decode(userKeysFull.nonceB64());
                byte[] recipientPublicKey = Base64.getDecoder().decode(userKeysFull.recipientPublicKeyB64());
                byte[] recipientPrivateKeyEncrypted = Base64.getDecoder().decode(userKeysFull.recipientPrivateKeyEncryptedB64());
                byte[] recipientPrivateKeyWrapNonce = Base64.getDecoder().decode(userKeysFull.recipientPrivateKeyWrapNonceB64());
                byte[] ownerSignPublicKey = Base64.getDecoder().decode(userKeysFull.ownerSignPublicKeyB64());
                byte[] encryptedOwnerSignPrivateKeyEncrypted = Base64.getDecoder().decode(userKeysFull.ownerSignPrivateKeyEncryptedB64());
                byte[] ownerSignPrivateKeyWrapNonce = Base64.getDecoder().decode(userKeysFull.ownerSignPrivateKeyWrapNonceB64());

                userKeyRepository.save(
                        new UserKey(
                                user,
                                wrappedMasterKey,
                                salt,
                                userKeysFull.kdfParams(),
                                nonce,
                                userKeysFull.vaultVersion(),
                                userKeysFull.kdfContexts(),
                                recipientPublicKey,
                                recipientPrivateKeyEncrypted,
                                recipientPrivateKeyWrapNonce,
                                ownerSignPublicKey,
                                encryptedOwnerSignPrivateKeyEncrypted,
                                ownerSignPrivateKeyWrapNonce
                        )
                );

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.USER_KEYS_CREATE,
                    "Naudotojo šifravimo raktai sėkmingai inicijuoti",
                    Map.of(
                            "vaultVersion", userKeysFull.vaultVersion()
                    )
            );
        }
        catch (IllegalArgumentException e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.USER_KEYS_CREATE,
                    "Nepavyko inicijuoti naudotojo šifravimo raktų: netinkamas Base64 formatas",
                    Map.of(
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Užklausoje pateiktas netinkamas Base64 formatas");
        }
        catch (DataIntegrityViolationException e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.USER_KEYS_CREATE,
                    "Nepavyko inicijuoti naudotojo šifravimo raktų: saugykla jau inicijuota",
                    Map.of(
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saugykla jau inicijuota!");
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.USER_KEYS_CREATE,
                    "Nepavyko inicijuoti naudotojo šifravimo raktų",
                    Map.of(
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    private UserKey getUserKeyOrThrow(UUID userId) {
        return userKeyRepository.findUserKeyByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojo raktai nerasti"));
    }

    public UserKeysFull getUserKeysFull(UUID userId){
      UserKey userKey = getUserKeyOrThrow(userId);

        return new UserKeysFull(
                Base64.getEncoder().encodeToString(userKey.getKdfSalt()),
                userKey.getKdfParams(),
                Base64.getEncoder().encodeToString(userKey.getEncryptedMasterKey()),
                Base64.getEncoder().encodeToString(userKey.getWrapNonce()),
                userKey.getVault_version(),
                userKey.getKdfContexts(),
                Base64.getEncoder().encodeToString(userKey.getRecipientPublicKey()),
                Base64.getEncoder().encodeToString(userKey.getEncryptedRecipientPrivateKey()),
                Base64.getEncoder().encodeToString(userKey.getRecipientPrivateKeyWrapNonce()),
                Base64.getEncoder().encodeToString(userKey.getOwnerSignPublicKey()),
                Base64.getEncoder().encodeToString(userKey.getEncryptedOwnerSignPrivateKey()),
                Base64.getEncoder().encodeToString(userKey.getOwnerSignPrivateKeyWrapNonce())
        );
    }

    public UserKeysBasic getUserKeys(UUID userId){
        UserKey userKey = getUserKeyOrThrow(userId);

        return new UserKeysBasic(
                Base64.getEncoder().encodeToString(userKey.getKdfSalt()),
                userKey.getKdfParams(),
                Base64.getEncoder().encodeToString(userKey.getEncryptedMasterKey()),
                Base64.getEncoder().encodeToString(userKey.getWrapNonce()),
                userKey.getVault_version(),
                userKey.getKdfContexts()
        );
    }

    public boolean checkForUserKeys(UUID userId){
        return userKeyRepository.existsByUserId(userId);
    }

    public List<RecipientPublicKey> getRecipientPublicKeys(List<UUID> recipientIds){
       return userKeyRepository.findAllByUserIdIn(recipientIds).stream().map(u ->
                new RecipientPublicKey(
                        u.getUser().getId(),
                        Base64.getEncoder().encodeToString(u.getRecipientPublicKey())
                )).toList();
    }

    public UserSignPrivateKey getOwnerSignPrivateKey(UUID userId){
        UserKey userKey = getUserKeyOrThrow(userId);
        return new UserSignPrivateKey(
                userId,
                Base64.getEncoder().encodeToString(userKey.getKdfSalt()),
                userKey.getKdfParams(),
                userKey.getVault_version(),
                userKey.getKdfContexts(),
                Base64.getEncoder().encodeToString(userKey.getEncryptedOwnerSignPrivateKey()),
                Base64.getEncoder().encodeToString(userKey.getOwnerSignPrivateKeyWrapNonce())
        );
    }

    public RecipientUnlockKeys getRecipientUnlockKeys(UUID userId) {
        UserKey userKey = getUserKeyOrThrow(userId);
        return new RecipientUnlockKeys(
                userId,
                Base64.getEncoder().encodeToString(userKey.getKdfSalt()),
                userKey.getKdfParams(),
                userKey.getVault_version(),
                userKey.getKdfContexts(),
                Base64.getEncoder().encodeToString(userKey.getRecipientPublicKey()),
                Base64.getEncoder().encodeToString(userKey.getEncryptedRecipientPrivateKey()),
                Base64.getEncoder().encodeToString(userKey.getRecipientPrivateKeyWrapNonce()),
                Base64.getEncoder().encodeToString(userKey.getOwnerSignPublicKey())
        );
    }
}
