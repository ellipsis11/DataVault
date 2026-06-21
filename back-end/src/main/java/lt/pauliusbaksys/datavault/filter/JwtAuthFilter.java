package lt.pauliusbaksys.datavault.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lt.pauliusbaksys.datavault.service.JwtService;
import lt.pauliusbaksys.datavault.service.LocalUserDetailsService;
import lt.pauliusbaksys.datavault.service.OAuthUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {


    private final JwtService jwtService;
    private final LocalUserDetailsService localUserDetailsService;
    private final OAuthUserDetailsService oAuthUserDetailsService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtService jwtService, LocalUserDetailsService localUserDetailsService, OAuthUserDetailsService oAuthUserDetailsService) {
        this.jwtService = jwtService;
        this.localUserDetailsService = localUserDetailsService;
        this.oAuthUserDetailsService = oAuthUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UserDetails user;
        String jwtToken = resolveToken(request);
        String path = request.getServletPath();

        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn(jwtToken != null ? "TOKEN YRA" : "TOKENAS TUSCIAS!");
        if (jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null){
            try {
                if (jwtService.isValid(jwtToken)){
                    String tokenSubject = jwtService.extractSubject(jwtToken);
                    log.info("tokenSubject: {}", tokenSubject);
                    if (jwtService.isOAuth2()) {user = oAuthUserDetailsService.loadUserByUsername(tokenSubject);}
                    else {user = localUserDetailsService.loadUserByUsername(tokenSubject);}
                    // Creating an Authentication object “this request is authenticated as this user”.
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            catch (Exception e){
                log.error("JwtAuthFilter: {}", e.toString());
                SecurityContextHolder.clearContext();
            }
        }
        // Continuing the filter chain
        filterChain.doFilter(request, response);
    }

    public String resolveToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies != null){
            for (Cookie c : cookies){
                if ("access_token".equals(c.getName())){
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
