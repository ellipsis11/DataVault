package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.dto.OauthProfileMapper;
import lt.pauliusbaksys.datavault.dto.OauthUserProfile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OAuthProfileProcessor {

    private OauthUserProfile profile;

    public void process(String provider, Map<String, Object> attrs){
        this.profile = OauthProfileMapper.mappers.get(provider).apply(attrs);
    }

    public OauthUserProfile getProfile() {
        return profile;
    }
}
