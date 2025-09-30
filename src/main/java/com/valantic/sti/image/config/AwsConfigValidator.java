package com.valantic.sti.image.config;

import com.valantic.sti.image.ImageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;


@Component
@ConditionalOnProperty(name = "aws.config.validation.enabled", havingValue = "true")
public class AwsConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(AwsConfigValidator.class);
    
    private final S3Client s3Client;
    private final ImageProperties imageProperties;

    public AwsConfigValidator(S3Client s3Client, ImageProperties imageProperties) {
        this.s3Client = s3Client;
        this.imageProperties = imageProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateAwsConfiguration() {
        log.info("Validating AWS configuration...");
        
        try {
            validateS3Buckets();
            log.info("AWS configuration validation completed successfully");
        } catch (Exception e) {
            log.error("AWS configuration validation failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid AWS configuration", e);
        }
    }

    private void validateS3Buckets() {
        s3Client.headBucket(HeadBucketRequest.builder()
            .bucket(imageProperties.bucketName())
            .build());
        log.debug("Main bucket '{}' is accessible", imageProperties.bucketName());
        
        s3Client.headBucket(HeadBucketRequest.builder()
            .bucket(imageProperties.thumbnailBucketName())
            .build());
        log.debug("Thumbnail bucket '{}' is accessible", imageProperties.thumbnailBucketName());
    }


}