package com.valantic.sti.image.health;

import com.valantic.sti.image.ImageProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component("s3")
public class S3HealthIndicator implements HealthIndicator {

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
            return Health.down()
                .withDetail("bucket", imageProperties.bucketName())
                .withDetail("error", "S3 bucket not accessible")
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}