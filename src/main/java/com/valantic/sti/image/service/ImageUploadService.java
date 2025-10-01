package com.valantic.sti.image.service;

import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.model.ImageDimensions;
import com.valantic.sti.image.model.ImageResponse;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Specialized service for image upload operations.
 * <p>
 * Responsibilities:
 * - Upload Workflow Orchestration (Sync/Async)
 * - Input Validation and Sanitization
 * - Metadata creation and persistence
 * - S3 upload coordination
 */
@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);

    private final S3StorageService s3StorageService;
    private final ImageProcessingService imageProcessingService;
    private final ImageMetadataService imageMetadataService;
    private final ImageUrlService imageUrlService;
    private final ImageValidationService imageValidationService;

    public ImageUploadService(S3StorageService s3StorageService,
                              ImageProcessingService imageProcessingService,
                              ImageMetadataService imageMetadataService,
                              ImageUrlService imageUrlService,
                              ImageValidationService imageValidationService) {
        this.s3StorageService = s3StorageService;
        this.imageProcessingService = imageProcessingService;
        this.imageMetadataService = imageMetadataService;
        this.imageUrlService = imageUrlService;
        this.imageValidationService = imageValidationService;
    }

    /**
     * Synchronous upload with full processing.
     */
    @Transactional
    @Timed("image.upload.sync")
    @Counted("image.upload.sync.requests")
    public ImageResponse uploadSync(MultipartFile file, String title, String description, List<String> tags) {
        return upload(file, title, description, tags, false);
    }

    /**
     * Asynchronous upload for large files.
     */
    @Transactional
    @Timed("image.upload.async")
    @Counted("image.upload.async.requests")
    public ImageResponse uploadAsync(MultipartFile file, String title, String description, List<String> tags) {
        return upload(file, title, description, tags, true);
    }

    /**
     * Centralized upload processing.
     */
    private ImageResponse upload(MultipartFile file, String title, String description, List<String> tags, boolean async) {
        imageValidationService.validateImageFile(file);
        imageValidationService.validateInputs(title, description, tags);

        String imageId = UUID.randomUUID().toString();
        String originalKey = "images/" + imageId + "/original";

        try {
            byte[] imageData = file.getBytes();
            ImageDimensions dimensions = imageProcessingService.getImageDimensions(imageData);

            ImageMetadata metadata = createImageMetadata(imageId, title, description, tags, file, dimensions, originalKey);
            imageMetadataService.save(metadata);

            if (async) {
                s3StorageService.uploadImageAsync(imageId, imageData, file.getContentType(), metadata);
                log.info("Image upload initiated: {} (async processing started)", imageId);
            } else {
                s3StorageService.uploadImage(originalKey, imageData, file.getContentType(), metadata);
                imageProcessingService.generateThumbnails(imageId, imageData, file.getContentType());
                log.info("Image uploaded successfully: {}", imageId);
            }

            return buildImageResponse(metadata);

        } catch (IOException e) {
            log.error("IO error during upload for image: {}", imageId, e);
            throw new RuntimeException(async ? "Upload preparation failed" : "Upload failed", e);
        }
    }

    private ImageMetadata createImageMetadata(String imageId, String title, String description, List<String> tags,
                                              MultipartFile file, ImageDimensions dimensions, String originalKey) {
        return new ImageMetadata(
            imageId, title, description,
            tags != null ? Set.copyOf(tags) : Set.of(),
            file.getContentType(), file.getSize(),
            dimensions.width(), dimensions.height(),
            originalKey, getCurrentUser()
        );
    }

    private ImageResponse buildImageResponse(ImageMetadata metadata) {
        return new ImageResponse(
            metadata.getImageId(),
            metadata.getTitle(),
            metadata.getDescription(),
            List.copyOf(metadata.getTags()),
            metadata.getContentType(),
            metadata.getFileSize(),
            new ImageDimensions(metadata.getWidth(), metadata.getHeight()),
            metadata.getCreatedAt(),
            metadata.getUpdatedAt(),
            metadata.getUploadedBy(),
            imageUrlService.buildImageUrls(metadata.getImageId())
        );
    }

    private String getCurrentUser() {
        // TODO: Spring Security Integration
        return "system";
    }
}
