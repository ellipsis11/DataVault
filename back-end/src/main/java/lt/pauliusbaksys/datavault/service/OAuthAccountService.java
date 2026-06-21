package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.OauthUserProfile;
import lt.pauliusbaksys.datavault.model.OAuthAccount;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.OAuthAccountRepository;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class OAuthAccountService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    public OAuthAccountService(UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    public OAuthAccount findOrCreateOAuthAccount(OauthUserProfile profile){
        return oAuthAccountRepository.findByProviderAndProviderUserId(profile.provider(), profile.userId())
                .orElseGet(() -> {
                    User user = userRepository.save(new User(profile.email()));
                    OAuthAccount acc = new OAuthAccount(user, profile.provider(), profile.userId(), profile.userName());
                    return oAuthAccountRepository.save(acc);
                });
    }
}
