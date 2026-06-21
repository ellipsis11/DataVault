package lt.pauliusbaksys.datavault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class FilebaseIPFSConfig {

    /**
     * Creating an S3 client for Filebase IPFS storage.
     * The endpoint override is required because Filebase is not the default AWS S3
     * service endpoint. Filebase exposes an S3-compatible API, so the AWS SDK can
     * be reused, but the client must be explicitly pointed to the Filebase endpoint.
     * <p>
     * So, the same S3 operations (PutObject, GetObject, etc.), are
     * sent to Filebase instead of Amazon S3.
     */
    @Bean("filebaseS3Client")
    public S3Client filebaseS3Client(FilebaseIpfsProperties properties){
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        properties.accessKey(),
                                        properties.secretKey()
                                )
                        )
                )
                /*
                 * Filebase is S3-compatible, but it works more reliably with path-style access.
                 *
                 * Path-style URL:
                 * https://s3.filebase.io/{bucket-name}/{object-key}
                 *
                 * AWS Virtual-hosted-style URL:
                 * https://{bucket-name}.s3.filebase.io/{object-key}
                 *
                 * AWS S3 usually supports virtual-hosted-style URLs without problems.
                 * But some S3-compatible providers, like Filebase, may require path-style access.
                 */
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }
}
