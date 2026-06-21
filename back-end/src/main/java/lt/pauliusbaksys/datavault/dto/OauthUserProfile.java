package lt.pauliusbaksys.datavault.dto;

public record OauthUserProfile(
        String provider,
        String userId,
        String userName,
        String email
){}