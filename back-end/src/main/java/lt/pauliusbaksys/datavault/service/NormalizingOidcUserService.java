package lt.pauliusbaksys.datavault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class NormalizingOidcUserService extends OidcUserService {

    private final OAuthProfileProcessor oAuthProfileProcessor;
    private static final Logger log = LoggerFactory.getLogger(NormalizingOidcUserService.class);

    public NormalizingOidcUserService(OAuthProfileProcessor oAuthProfileProcessor) {
        this.oAuthProfileProcessor = oAuthProfileProcessor;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser user = super.loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId();
        oAuthProfileProcessor.process(provider, user.getAttributes());
        return user;
    }
}
