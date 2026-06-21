package lt.pauliusbaksys.datavault.security;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lt.pauliusbaksys.datavault.dto.OauthUserProfile;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.model.OAuthAccount;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import lt.pauliusbaksys.datavault.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;


@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthAccountService oAuthAccountService;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    private final long expirationMin;
    private final OAuthProfileProcessor processor;
    private final ConditionalReleaseService conditionalReleaseService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(OAuthAccountService oAuthAccountService, JwtService jwtService, @Value("${app.jwt.expirationMin}") long expirationMin, OAuthProfileProcessor processor, ConditionalReleaseService conditionalReleaseService, AuditLogService auditLogService, UserRepository userRepository) {
        this.oAuthAccountService = oAuthAccountService;
        this.jwtService = jwtService;
        this.expirationMin = expirationMin;
        this.processor = processor;
        this.conditionalReleaseService = conditionalReleaseService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // Extracts the authenticated OAuth2 user information returned by the OAuth provider
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.warn(oAuth2User != null ? "oAuth2User YRA" : "oAuth2User tuscias");

        if (oAuth2User != null) {
            OauthUserProfile profile = processor.getProfile();
            log.warn("OAuth2LoginSuccessHandler: {}", profile);
            OAuthAccount oAuthAccount = oAuthAccountService.findOrCreateOAuthAccount(profile);
            log.info("oAuthAccount: {}", oAuthAccount);
            log.info("Profile: {}", profile);

            User user = oAuthAccount.getUser();
            UUID userId = user.getId();

            user.setLastLoginAt(OffsetDateTime.now());

            userRepository.save(user);

            conditionalReleaseService.refreshHeartbeatForUser(userId);
            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.LOGIN,
                    "Naudotojas sėkmingai prisijungė naudodamas %s OAuth".formatted(oAuthAccount.getProvider())
            );

            jwtService.setOAuth2(true);
            String jwt = jwtService.generateToken(profile.provider() + ":" + profile.userId());
            ResponseCookie cookie = ResponseCookie.from("access_token", jwt)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofMinutes(expirationMin))
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());
            response.sendRedirect("http://localhost:5173/dashboard");
        }
        else {
            auditLogService.logAnonymous(
                    "?",
                    AuditLevel.ALERT,
                    AuditAction.LOGIN,
                    "Naudotojui nepavyko prisijungti per OAuth"
            );

            /*
            * This means that authentication was completed, but Spring Security did not provide
            * the expected OAuth2 user details needed to continue the login process.
            * Principal is the logged-in user's OAuth2 profile, from which the email, name, and other information returned by the provider are taken.
            */
            response.sendRedirect("http://localhost:5173/auth?error=oauth_principal_null");
        }
    }
}
