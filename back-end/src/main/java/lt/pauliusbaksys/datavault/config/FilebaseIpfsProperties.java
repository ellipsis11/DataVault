package lt.pauliusbaksys.datavault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filebase.s3")
public record FilebaseIpfsProperties(
        String region,
        String endpoint,
        String bucket,
        String accessKey,
        String secretKey
) {}