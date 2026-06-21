package lt.pauliusbaksys.datavault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuditLogHashService {

    private final String hmacSecret;

    public AuditLogHashService(@Value("${audit.log.hmac-secret}") String hmacSecret){
        this.hmacSecret = hmacSecret;
    }

    public byte[] calculateHash(String data){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(keySpec);

            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e){
            throw new IllegalStateException("Nepavyko apskaičiuoti audito registro HMAC!", e);
        }
    }

    /**
     * Using {@link MessageDigest#isEqual(byte[], byte[])} instead of {@link java.util.Arrays#equals(byte[], byte[])}
     * to reduce the risk of timing attacks.
     * <br>
     * Regular byte array comparison may return as soon as the first different byte is found,
     * which can theoretically leak information through response timing.
     */
    public boolean verifyHash(String data, byte[] expectedHash){
        if (expectedHash == null || expectedHash.length == 0){
            return false;
        }

        byte[] calculatedHash = calculateHash(data);

        return MessageDigest.isEqual(
                calculatedHash,
                expectedHash
        );
    }
}