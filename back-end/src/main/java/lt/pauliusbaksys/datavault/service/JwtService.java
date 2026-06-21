package lt.pauliusbaksys.datavault.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lt.pauliusbaksys.datavault.dto.JwtKeys;
import lt.pauliusbaksys.datavault.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private boolean OAuth2 = false;
    private final long expirationMs;
    private final JwtKeys jwtKeys;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);


    public JwtService(@Value("${app.jwt.expirationMin}") long expirationMin, JwtKeys jwtKeys) {
        this.expirationMs = 1000L * 60 * expirationMin;
        this.jwtKeys = jwtKeys;
    }

    private PublicKey publicKey() {
        return jwtKeys.publicKey();
    }
    private PrivateKey privateKey() {
        return jwtKeys.privateKey();
    }

    public String generateToken(String subject) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(expirationMs)))
                .and()
                .signWith(privateKey(), Jwts.SIG.ES256)
                .compact();
    }

    public Optional<Claims> extractAllClaims(String jwtToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey())
                    .build()
                    // Validates the token, then parses the claims
                    .parseSignedClaims(jwtToken)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty(); // Returns an empty optional
        }
    }

    public boolean isValid(String jwtToken ){
        return extractAllClaims(jwtToken).isPresent();
    }

    public String extractSubject(String jwtToken) {
        return extractAllClaims(jwtToken)
                .map(claims -> claims.getSubject())
                .orElse(null);
    }

    public boolean isOAuth2() {
        return OAuth2;
    }

    public void setOAuth2(boolean OAuth2) {
        this.OAuth2 = OAuth2;
    }
}




