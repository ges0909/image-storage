package com.valantic.sti.image.health;

import com.valantic.sti.image.ImageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component("s3")
@ConditionalOnProperty(name = "management.health.s3.enabled", havingValue = "true")
public class S3HealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(S3HealthIndicator.class);

    private final S3Client s3Client;
    private final ImageProperties imageProperties;

    public S3HealthIndicator(S3Client s3Client, ImageProperties imageProperties) {
        this.s3Client = s3Client;
        this.imageProperties = imageProperties;
    }

    @Override
    public Health health() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(imageProperties.bucketName())
                .build());

            return Health.up()
                .withDetail("bucket", imageProperties.bucketName())
                .withDetail("status", "accessible")
                .build();
        } catch (Exception e) {
            log.warn("S3 health check failed for bucket: {}", imageProperties.bucketName(), e);
            return Health.down()
                .withDetail("bucket", imageProperties.bucketName())
                .withDetail("error", "S3 bucket not accessible")
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
