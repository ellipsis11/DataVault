package lt.pauliusbaksys.datavault.security;
import lt.pauliusbaksys.datavault.filter.JwtAuthFilter;
import lt.pauliusbaksys.datavault.service.LocalUserDetailsService;
import lt.pauliusbaksys.datavault.service.NormalizingOAuth2UserService;
import lt.pauliusbaksys.datavault.service.NormalizingOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final LocalUserDetailsService localUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final NormalizingOAuth2UserService normalizingOAuth2UserService;
    private final NormalizingOidcUserService normalizingOidcUserService;

    public SecurityConfig(LocalUserDetailsService localUserDetailsService, PasswordEncoder passwordEncoder, JwtAuthFilter jwtAuthFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, OAuth2LoginFailureHandler oAuth2LoginFailureHandler, NormalizingOAuth2UserService normalizingOAuth2UserService, NormalizingOidcUserService normalizingOidcUserService){
        this.localUserDetailsService = localUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.normalizingOAuth2UserService = normalizingOAuth2UserService;
        this.normalizingOidcUserService = normalizingOidcUserService;
    }

    // Cross-Origin Resource Sharing
    @Bean
    CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:5173"));
        corsConfig.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN"));
        // Allow cookies (needed for httpOnly JWT cookie / sessions)
        corsConfig.setAllowCredentials(true);
        // how long (in seconds) it can cache the CORS “preflight” (OPTIONS) response.
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Appling CORS config to all endpoints in backend (/**).
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain (HttpSecurity http){
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> {
                    oauth.userInfoEndpoint(u -> u
                            .userService(normalizingOAuth2UserService)
                            .oidcUserService(normalizingOidcUserService)
                    );
                    oauth.successHandler(oAuth2LoginSuccessHandler);
                    oauth.failureHandler((oAuth2LoginFailureHandler));
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                org.springframework.http.HttpStatus.UNAUTHORIZED
                        ))
                )
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(localUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config){
        return config.getAuthenticationManager();
    }
}
