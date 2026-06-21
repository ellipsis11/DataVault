package lt.pauliusbaksys.datavault.security;

import lt.pauliusbaksys.datavault.dto.JwtKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

@Configuration
public class JwtKeyConfig {

    // Injecting config values -> @Value("${...}") tells Spring: read this property from application.properties / env / config.
    @Value("${app.jwt.keystore}")
    private Resource keystore;

    @Value("${app.jwt.storePassword}")
    private String storePassword;

    @Value("${app.jwt.keyAlias}")
    private String keyAlias;

    @Value("${app.jwt.keyPassword}")
    private String keyPassword;

    @Bean
    public JwtKeys jwtKeys() throws Exception{
        // Creating a Java KeyStore object configured for the PKCS#12 keystore format
        KeyStore ks = KeyStore.getInstance("PKCS12");
        // Opening and loading the keystore file into the KeyStore ks
        try (InputStream is = keystore.getInputStream()){
            ks.load(is, storePassword.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, keyPassword.toCharArray());
        PublicKey publicKey = ks.getCertificate(keyAlias).getPublicKey();

        return new JwtKeys(privateKey, publicKey);
    }
}
