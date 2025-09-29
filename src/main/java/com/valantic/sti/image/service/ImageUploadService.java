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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 📤 Spezialisierter Service für Bild-Upload-Operationen.
 * <p>
 * Verantwortlichkeiten:
 * - Upload-Workflow-Orchestrierung (Sync/Async)
 * - Input-Validierung und Sanitization
 * - Metadaten-Erstellung und -Persistierung
 * - S3-Upload-Koordination
 */
@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);

    private final S3StorageService s3StorageService;
    private final ImageProcessingService imageProcessingService;
    private final ImageMetadataService imageMetadataService;
    private final ImageUrlService imageUrlService;
    private final ImageValidationService imageValidationService;
    private final AsyncImageService asyncImageService;

    public ImageUploadService(S3StorageService s3StorageService,
                              ImageProcessingService imageProcessingService,
                              ImageMetadataService imageMetadataService,
                              ImageUrlService imageUrlService,
                              ImageValidationService imageValidationService,
                              AsyncImageService asyncImageService) {
        this.s3StorageService = s3StorageService;
        this.imageProcessingService = imageProcessingService;
        this.imageMetadataService = imageMetadataService;
        this.imageUrlService = imageUrlService;
        this.imageValidationService = imageValidationService;
        this.asyncImageService = asyncImageService;
    }

    /**
     * 📤 Synchroner Upload mit vollständiger Verarbeitung.
     */
    @Transactional
    @Timed("image.upload.sync")
    @Counted("image.upload.sync.requests")
    public ImageResponse uploadSync(MultipartFile file, String title, String description, List<String> tags) {
        return processUpload(file, title, description, tags, false);
    }

    /**
     * ⚡ Asynchroner Upload für große Dateien.
     */
    @Transactional
    @Timed("image.upload.async")
    @Counted("image.upload.async.requests")
    public ImageResponse uploadAsync(MultipartFile file, String title, String description, List<String> tags) {
        return processUpload(file, title, description, tags, true);
    }

    /**
     * 🔄 Zentrale Upload-Verarbeitung.
     */
    private ImageResponse processUpload(MultipartFile file, String title, String description, List<String> tags, boolean async) {
        validateUploadInputs(file, title, description, tags);

        String imageId = UUID.randomUUID().toString();
        String originalKey = "images/" + imageId + "/original";

        try {
            byte[] imageData = file.getBytes();
            ImageDimensions dimensions = imageProcessingService.getImageDimensions(imageData);

            ImageMetadata metadata = createImageMetadata(imageId, title, description, tags, file, dimensions, originalKey);
            imageMetadataService.save(metadata);

            if (async) {
                asyncImageService.uploadImage(imageId, imageData, file.getContentType(), metadata);
                log.info("Image upload initiated: {} (async processing started)", imageId);
            } else {
                processSyncUpload(imageId, imageData, file.getContentType(), originalKey, title, description, tags);
                log.info("Image uploaded successfully: {}", imageId);
            }

            return buildImageResponse(metadata);

        } catch (IOException e) {
            log.error("IO error during upload for image: {}", imageId, e);
            throw new RuntimeException(async ? "Upload preparation failed" : "Upload failed", e);
        }
    }

    private void validateUploadInputs(MultipartFile file, String title, String description, List<String> tags) {
        imageValidationService.validateImageFile(file);
        imageValidationService.validateInputs(title, description, tags);
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

    private void processSyncUpload(String imageId, byte[] imageData, String contentType,
                                   String originalKey, String title, String description, List<String> tags) {
        Map<String, String> metadata = buildMetadata(title, description, tags);
        s3StorageService.uploadImage(originalKey, imageData, contentType, metadata);
        imageProcessingService.generateThumbnails(imageId, imageData, contentType);
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

    private Map<String, String> buildMetadata(String title, String description, List<String> tags) {
        Map<String, String> metadata = new HashMap<>();
        if (title != null) metadata.put("title", title);
        if (description != null) metadata.put("description", description);
        if (tags != null && !tags.isEmpty()) metadata.put("tags", String.join(",", tags));
        metadata.put("uploaded-at", LocalDateTime.now().toString());
        return metadata;
    }

    private String getCurrentUser() {
        return "system"; // TODO: Spring Security Integration
    }
}
