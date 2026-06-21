package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.filter.JwtAuthFilter;
import lt.pauliusbaksys.datavault.model.OAuthAccount;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.OAuthAccountRepository;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class OAuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);


    public OAuthUserDetailsService(UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        int i = username.indexOf(":");
        if (i < 0) throw new IllegalArgumentException("Missing ':'");
        OAuthAccount oAuthAccount = oAuthAccountRepository.findByProviderAndProviderUserId(username.substring(0, i), username.substring(i + 1))
                .orElseThrow(() -> new UsernameNotFoundException("OAuth naudotojas nerastas: " + username));
        User user = oAuthAccount.getUser();
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(null)
                .roles(user.getRole().name())
                .build();
    }
}
