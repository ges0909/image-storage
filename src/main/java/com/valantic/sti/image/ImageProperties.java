package com.valantic.sti.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@ConfigurationProperties(prefix = "image")
@Validated
public record ImageProperties(
    @NotBlank String bucketName,
    @NotBlank String thumbnailBucketName,
    @NotBlank String kmsKeyId,
    String cloudfrontDomain,
    @Positive long maxFileSize,
    @NotBlank String region,
    @Positive @Max(60) int urlExpirationMinutes,
    @NotNull int[] thumbnailSizes,
    @NotNull Set<String> supportedTypes,
    @Positive @Max(1000) int maxResults,
    @NotBlank String keyPrefix
) {
    public ImageProperties {
        // Validate thumbnail sizes are positive
        for (int size : thumbnailSizes) {
            if (size <= 0) {
                throw new IllegalArgumentException("Thumbnail sizes must be positive");
            }
        }
    }

    public Set<String> allowedContentTypes() {
        return supportedTypes;
    }
}

