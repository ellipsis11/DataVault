package lt.pauliusbaksys.datavault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

/**
 * Creating an AWS S3 client object to communicate with Amazon S3 storage.
 * The client uses the configured AWS access keys to sign requests and sends
 * file upload, download and delete operations to the selected S3 bucket over HTTPS.
 * <p>
 * Injecting AwsS3Properties directly into this bean method instead of using constructor injection.
 * Because the properties are only used once – for S3Clinet bean creation.
**/
    @Bean
    public S3Client s3Client(AwsS3Properties properties){
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        properties.accessKey(),
                                        properties.secretKey()
                                )
                        )
                )
                .build();
    }
}
