package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.model.ImageUrls;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class ImageUrlService {

    private static final Duration MAX_PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    private final S3Presigner s3Presigner;
    private final ImageProperties imageProperties;

    public ImageUrlService(S3Presigner s3Presigner, ImageProperties imageProperties) {
        this.s3Presigner = s3Presigner;
        this.imageProperties = imageProperties;
    }

    public ImageUrls buildImageUrls(String imageId) {
        String originalUrl = generatePresignedUrl(
            imageProperties.bucketName(),
            "images/" + imageId + "/original"
        );

        String thumbnail150 = generatePresignedUrl(
            imageProperties.thumbnailBucketName(),
            "thumbnails/" + imageId + "/150.webp"
        );

        String thumbnail300 = generatePresignedUrl(
            imageProperties.thumbnailBucketName(),
            "thumbnails/" + imageId + "/300.webp"
        );

        String thumbnail600 = generatePresignedUrl(
            imageProperties.thumbnailBucketName(),
            "thumbnails/" + imageId + "/600.webp"
        );

        return new ImageUrls(originalUrl, thumbnail150, thumbnail300, thumbnail600);
    }

    public String generatePresignedUrl(String bucketName, String key) {
        return generatePresignedUrl(bucketName, key, imageProperties.urlExpirationMinutes());
    }

    public String generatePresignedUrl(String bucketName, String key, int durationMinutes) {
        Duration duration = Duration.ofMinutes(Math.min(durationMinutes, (int) MAX_PRESIGNED_URL_DURATION.toMinutes()));

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(duration)
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build())
            .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
