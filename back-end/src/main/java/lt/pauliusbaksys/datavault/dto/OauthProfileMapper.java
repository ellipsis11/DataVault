package lt.pauliusbaksys.datavault.dto;
import java.util.Map;
import java.util.function.Function;

public final class OauthProfileMapper {

    // Mapping each OAuth provider to a function that converts its attributes into a user profile.
    public static final Map<String, Function<Map<String, Object>, OauthUserProfile>> mappers =
            Map.of(
                    "github", attrs -> new OauthUserProfile(
                            "github",
                            asString(attrs.get("id")),
                            asString(attrs.get("login")),
                            asString(attrs.get("email"))
                    ),
                    "google", attrs -> new OauthUserProfile(
                            "google",
                            asString(attrs.get("sub")),
                            asString(attrs.get("name")),
                            asString(attrs.get("email"))
                    )
            );

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
