package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lt.pauliusbaksys.datavault.dto.AuthUser;
import lt.pauliusbaksys.datavault.dto.FrontRequest;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.filter.JwtAuthFilter;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import lt.pauliusbaksys.datavault.service.AuditLogService;
import lt.pauliusbaksys.datavault.service.ConditionalReleaseService;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final long expirationMin;
    private final CurrentUserService currentUserService;
    private final ConditionalReleaseService conditionalReleaseService;
    private final AuditLogService auditLogService;
    private final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserRepository userRepository, UserService userService, @Value("${app.jwt.expirationMin}") long expirationMin, CurrentUserService currentUserService, ConditionalReleaseService conditionalReleaseService, AuditLogService auditLogService){
        this.userRepository = userRepository;
        this.userService = userService;
        this.expirationMin = expirationMin;
        this.currentUserService = currentUserService;
        this.conditionalReleaseService = conditionalReleaseService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> regUser(@Valid @RequestBody FrontRequest user){
        try {
            userService.createUser(user);
            auditLogService.logAnonymous(
                    user.email(),
                    AuditLevel.INFO,
                    AuditAction.REGISTER,
                    "Naudotojas sėkmingai užregistruotas"
            );

            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        catch (Exception e){
            auditLogService.logAnonymous(
                    user.email(),
                    AuditLevel.ALERT,
                    AuditAction.REGISTER,
                    "Naudotojo registracija nepavyko",
                    Map.of("reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> logInUser(@Valid @RequestBody FrontRequest user){

        try {
            String jwt = userService.authUser(user);
            UUID userId = currentUserService.getCurrentUserId(jwt);

            User cUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas!"));

            cUser.setLastLoginAt(OffsetDateTime.now());

            userRepository.save(cUser);

            conditionalReleaseService.refreshHeartbeatForUser(userId);
            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.LOGIN,
                    "Naudotojas sėkmingai prisijungė naudodamas prisijungimo formą"
            );

            ResponseCookie cookie = ResponseCookie.from("access_token", jwt)
                    .httpOnly(true)
                    .secure(false)          // True in HTTPS.
                    .sameSite("Lax")        // None for cross-site + HTTPS.
                    .path("/")
                    .maxAge(Duration.ofMinutes(expirationMin))
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new AuthUser(
                            cUser.getId(),
                            cUser.getEmail(),
                            cUser.getRole()
                    ));
        }
        catch (Exception e){
            auditLogService.logAnonymous(
                    user.email(),
                    AuditLevel.ALERT,
                    AuditAction.LOGIN,
                    "Naudotojui nepavyko prisijungti naudojant prisijungimo formą",
                    Map.of("reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    @GetMapping("/me")
    public AuthUser getMe(HttpServletRequest req){
        User user = userRepository.findById(currentUserService.getCurrentUserId(req))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Toks naudotojas neegzistuoja!"));
        return new AuthUser(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logOutUser(HttpServletRequest req){
        auditLogService.log(
                currentUserService.getCurrentUserId(req),
                AuditLevel.INFO,
                AuditAction.LOGOUT,
                "Naudotojas atsijungė"
        );

        ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }
}