package lt.pauliusbaksys.datavault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class NormalizingOAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger log = LoggerFactory.getLogger(NormalizingOAuth2UserService.class);
    private final OAuthProfileProcessor oAuthProfileProcessor;

    public NormalizingOAuth2UserService(OAuthProfileProcessor oAuthProfileProcessor) {
        this.oAuthProfileProcessor = oAuthProfileProcessor;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        oAuthProfileProcessor.process(provider, user.getAttributes());
        return user;
    }
}
