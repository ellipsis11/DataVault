package lt.pauliusbaksys.datavault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Mapping properties with the "aws.s3" prefix from application.properties to this record.
@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(
        String region,
        String bucket,
        String accessKey,
        String secretKey
) {}