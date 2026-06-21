package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.model.*;
import lt.pauliusbaksys.datavault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogRepository auditLogRepository;
    private final UserTelegramLinkRepository telegramRepo;
    private final UserKeyRepository userKeyRepository;
    private final ConditionalReleaseRepository conditionalReleaseRepository;
    private final PolicyRecipientRepository policyRecipientRepository;
    private final FileStorageRepository fileStorageRepository;
    private final ObjectStorageResolver objectStorageResolver;
    private final AuditLogService auditLogService;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);


    public UserService(UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, AuditLogRepository auditLogRepository, UserTelegramLinkRepository telegramRepo, UserKeyRepository userKeyRepository, ConditionalReleaseRepository conditionalReleaseRepository, PolicyRecipientRepository policyRecipientRepository, FileStorageRepository fileStorageRepository, ObjectStorageResolver objectStorageResolver, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogRepository = auditLogRepository;
        this.telegramRepo = telegramRepo;
        this.userKeyRepository = userKeyRepository;
        this.conditionalReleaseRepository = conditionalReleaseRepository;
        this.policyRecipientRepository = policyRecipientRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.objectStorageResolver = objectStorageResolver;
        this.auditLogService = auditLogService;
    }

    public String authUser(FrontRequest req) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );

            if (!authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Neteisingas el. paštas arba slaptažodis!");
            }

            userRepository.findUserByEmail(req.email())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Neteisingas el. paštas arba slaptažodis!"));

            jwtService.setOAuth2(false);
            return jwtService.generateToken(req.email());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Neteisingas el. paštas arba slaptažodis!");
        }
    }

    public void createUser(FrontRequest req) {
        if (!userRepository.existsByEmail(req.email())) {
            User user = new User();
            user.setEmail(req.email());
            user.setPasswordHash(passwordEncoder.encode(req.password()));
            userRepository.save(user);
        } else{
            // Avoiding user enumeration by not revealing whether the email already exists.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registracija nepavyko!");
        }
    }

    public UUID getUserId(String jwt) {
        String subject = jwtService.extractSubject(jwt);
        if (subject.contains(":")) {
            String[] parts = subject.split(":", 2);
            OAuthAccount acc = oAuthAccountRepository.findByProviderAndProviderUserId(parts[0], parts[1])
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth naudotojas nerastas: " + subject));

            return acc.getUser().getId();
        } else {
            User user = userRepository.findUserByEmail(subject)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas: " + subject));

            return user.getId();
        }
    }

    public List<UserSearchItem> searchUserSearchItems(String query) {
        String q = query == null ? "" : query.trim();

        return userRepository.findTop10ByEmailStartingWithIgnoreCase(q)
                .stream()
                .map(user -> new UserSearchItem(user.getId(), user.getEmail()))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserPage getUsersForAdmin(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.searchUsers(query, pageable);

        return new AdminUserPage(
                userPage.getContent().stream().map(
                        user -> {
                            return new AdminUserListItem(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getRole(),
                                    user.getLastLoginAt(),
                                    user.getCreatedAt(),
                                    auditLogRepository.countByUser_Id(user.getId())
                            );
                        }
                ).toList(),
                userPage.getTotalElements()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserDetails getUserDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas!"));

        List<AuditLog> top10UserLogs = auditLogRepository.findTop10ByUser_IdOrderByCreatedAtDesc(userId);
        Optional<UserTelegramLink> telegram = telegramRepo.findById(userId);
        Optional<UserKey> userKey = userKeyRepository.findUserKeyByUserId(userId);

        return new AdminUserDetails(
                userId,
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                telegram.isPresent(),
                telegram.map(t -> t.getTelegramUsername()).orElse(null),
                telegram.map(t -> t.getConnectedAt()).orElse(null),
                userKey.isPresent(),
                userKey.map(uk -> uk.getCreatedAt()).orElse(null),
                auditLogRepository.countByUser_Id(userId),
                conditionalReleaseRepository.countByOwner_Id(userId),
                policyRecipientRepository.countByRecipient_Id(userId),
                fileStorageRepository.countByOwner_Id(userId),
                top10UserLogs.stream().map(
                        log ->
                                new AuditLogListItem(
                                        log.getId(),
                                        log.getAuditLevel(),
                                        log.getActionType(),
                                        log.getMessage(),
                                        log.getCreatedAt(),
                                        true
                                )
                ).toList()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID userId, UUID currentAdminId) {
        if (userId.equals(currentAdminId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Savo paskyros ištrinti negalite!");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas!"));

        List<File> files = fileStorageRepository.findAllByOwner_Id(userId);

        try {
            for (File file : files) {
                ObjectStorageService service = objectStorageResolver.resolve(file.getStorageType());
                service.delete(file.getStorageRef());
            }

            userRepository.delete(user);

            auditLogService.log(
                    currentAdminId,
                    AuditLevel.ALERT,
                    AuditAction.USER_DELETE,
                    "Administratorius ištrynė naudotojo paskyrą",
                    Map.of(
                            "deletedUserId", userId,
                            "deletedUserEmail", user.getEmail(),
                            "deletedFilesCount", files.size()
                    )
            );
        }
        catch (RuntimeException e) {
            auditLogService.log(
                    currentAdminId,
                    AuditLevel.ALERT,
                    AuditAction.USER_DELETE,
                    "Nepavyko ištrinti naudotojo paskyros",
                    Map.of(
                            "targetUserId", userId,
                            "targetUserEmail", user.getEmail(),
                            "reason", e.getClass().getSimpleName()
                    )
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Nepavyko ištrinti naudotojo paskyros!",
                    e
            );
        }
    }
}

