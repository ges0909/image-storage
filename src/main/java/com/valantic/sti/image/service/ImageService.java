package com.valantic.sti.image.service;

import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.model.ImageAnalytics;
import com.valantic.sti.image.model.ImageDimensions;
import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.ImageSize;
import com.valantic.sti.image.model.ImageStats;
import com.valantic.sti.image.model.ImageUpdateRequest;
import com.valantic.sti.image.model.ImageVersion;
import com.valantic.sti.image.model.SearchRequest;
import com.valantic.sti.image.model.SearchResponse;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central orchestration layer for all machine vision-related operations.
 * <p>
 * This class of service coordinates the cooperation between specialized services:
 * - S3StorageService: AWS S3 Upload/Download
 * - ImageProcessingService: Thumbnail generation and image processing
 * - ImageMetadataService: Database operations for metadata
 * - ImageUrlService: Signed URL generation
 * - ImageValidationService: Input Validation and Sanitization
 * - AsyncImageService: Asynchronous background processing
 * <p>
 * Performance Features:
 * - Async upload for large files (10-100 MB)
 * - Micrometer metrics for monitoring
 * - Transactional Security
 * - Caching via ImageMetadataService
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final ImageUploadService imageUploadService;
    private final ImageMetadataService imageMetadataService;
    private final ImageUrlService imageUrlService;
    private final S3StorageService s3StorageService;
    private final com.valantic.sti.image.ImageProperties imageProperties;

    public ImageService(ImageUploadService imageUploadService,
                        ImageMetadataService imageMetadataService,
                        ImageUrlService imageUrlService,
                        S3StorageService s3StorageService,
                        com.valantic.sti.image.ImageProperties imageProperties) {
        this.imageUploadService = imageUploadService;
        this.imageMetadataService = imageMetadataService;
        this.imageUrlService = imageUrlService;
        this.s3StorageService = s3StorageService;
        this.imageProperties = imageProperties;
    }

    /**
     * Synchronous image upload with full processing. For large files (>10 MB) use uploadImageAsync()!
     */
    @Timed("image.upload.sync")
    @Counted("image.upload.requests")
    public ImageResponse uploadImage(MultipartFile file, String title, String description, List<String> tags) {
        return imageUploadService.uploadSync(file, title, description, tags);
    }

    /**
     * Asynchronous image upload for optimal performance with large files.
     */
    @Timed("image.upload.async")
    @Counted("image.upload.async.requests")
    public ImageResponse uploadImageAsync(MultipartFile file, String title, String description, List<String> tags) {
        return imageUploadService.uploadAsync(file, title, description, tags);
    }

    /**
     * Metadata retrieval with Redis caching (1h TTL).
     */
    public ImageResponse getImageMetadata(String imageId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        return buildImageResponse(metadata);
    }

    /**
     * Metadata update with partial update.
     */
    @Transactional
    @Timed("image.update")
    @Counted("image.update.requests")
    public ImageResponse updateImageMetadata(String imageId, ImageUpdateRequest request) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);

        if (request.title() != null) metadata.setTitle(request.title());
        if (request.description() != null) metadata.setDescription(request.description());
        if (request.tags() != null) metadata.setTags(Set.copyOf(request.tags()));

        ImageMetadata updated = imageMetadataService.save(metadata);
        log.info("Image metadata updated: {}", imageId);

        return buildImageResponse(updated);
    }

    /**
     * Database-based search, significantly faster than S3 ListObjects.
     */
    public SearchResponse searchImages(SearchRequest request) {
        return imageMetadataService.searchImages(request);
    }

    /**
     * Generate signed URLs for secure, time-limited access.
     */
    @Timed("image.url.generate")
    public String generateSignedUrl(String imageId, ImageSize size, Duration expiration) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        String key = size == ImageSize.ORIGINAL
            ? metadata.getS3Key()
            : "thumbnails/" + imageId + "/" + getSizePixels(size) + ".webp";
        String bucketName = size == ImageSize.ORIGINAL ?
            imageProperties.bucketName() : imageProperties.thumbnailBucketName();
        return imageUrlService.generatePresignedUrl(bucketName, key, (int) expiration.toMinutes());
    }

    /**
     * Thumbnail URL with default validity (15min).
     */
    public String getThumbnailUrl(String imageId, ImageSize size) {
        if (size == ImageSize.ORIGINAL) {
            throw new IllegalArgumentException("Use generateSignedUrl for original images");
        }
        String key = "thumbnails/" + imageId + "/" + getSizePixels(size) + ".webp";
        return imageUrlService.generatePresignedUrl(imageProperties.thumbnailBucketName(), key);
    }

    /**
     * Paginated image list, sorted by creation date.
     */
    @Timed("image.list")
    public List<ImageResponse> listImages(int page, int size) {
        SearchRequest request = new SearchRequest(null, null, null, page, size, "createdAt", "desc");
        return searchImages(request).images();
    }

    /**
     * Retrieves all S3 object versions for an image.
     */
    @Timed("image.versions.get")
    public List<ImageVersion> getImageVersions(String imageId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        return s3StorageService.getObjectVersions(metadata.getS3Key());
    }

    /**
     * Restores a specific S3 object version as the current version.
     */
    @Transactional
    @Timed("image.version.restore")
    @Counted("image.version.restore.requests")
    public ImageResponse restoreVersion(String imageId, String versionId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);

        // Restore S3 object version by copying the specific version to current
        s3StorageService.restoreVersion(metadata.getS3Key(), versionId);

        // Update metadata with restoration timestamp
        metadata.setUpdatedAt(java.time.LocalDateTime.now());
        ImageMetadata updated = imageMetadataService.save(metadata);

        log.info("Version {} restored for image: {}", versionId, imageId);
        return buildImageResponse(updated);
    }

    /**
     * Adds new tags to an existing image.
     */
    @Transactional
    @Timed("image.tags.add")
    @Counted("image.tags.add.requests")
    public void addTags(String imageId, List<String> tags) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        Set<String> currentTags = new java.util.HashSet<>(metadata.getTags());
        currentTags.addAll(tags);
        metadata.setTags(currentTags);
        imageMetadataService.save(metadata);
        log.info("Added {} tags to image: {}", tags.size(), imageId);
    }

    /**
     * Removes specific tags from an image.
     */
    @Transactional
    @Timed("image.tags.remove")
    @Counted("image.tags.remove.requests")
    public void removeTags(String imageId, List<String> tags) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        Set<String> currentTags = new java.util.HashSet<>(metadata.getTags());
        tags.forEach(currentTags::remove);
        metadata.setTags(currentTags);
        imageMetadataService.save(metadata);
        log.info("Removed {} tags from image: {}", tags.size(), imageId);
    }

    /**
     * Retrieves global image storage statistics.
     */
    @Timed("image.stats.get")
    public ImageStats getImageStats() {
        // TODO: Implement proper statistics calculation from database
        return new ImageStats(
            0L, 0L, Map.of(), Map.of(), 0L
        );
    }

    /**
     * Retrieves analytics data for a specific image.
     */
    @Timed("image.analytics.get")
    public ImageAnalytics getImageAnalytics(String imageId) {
        imageMetadataService.findById(imageId);
        // TODO: Implement proper analytics tracking
        return new ImageAnalytics(
            imageId, 0L, 0L, null, List.of()
        );
    }

    /**
     * Full image deletion: S3 objects + metadata.
     */
    @Transactional
    public void deleteImage(String imageId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        s3StorageService.deleteImage(metadata.getS3Key());
        imageMetadataService.deleteById(imageId);
        log.info("Image deleted: {}", imageId);
    }

    /**
     * Batch deletion of multiple images with error handling.
     */
    @Transactional
    @Timed("image.batch.delete")
    @Counted("image.batch.delete.requests")
    public void batchDeleteImages(List<String> imageIds) {
        for (String imageId : imageIds) {
            try {
                ImageMetadata metadata = imageMetadataService.findById(imageId);
                s3StorageService.deleteImage(metadata.getS3Key());
                imageMetadataService.deleteById(imageId);
            } catch (Exception e) {
                log.error("Failed to delete image: {}", imageId, e);
            }
        }
        log.info("Batch deleted {} images", imageIds.size());
    }

    private int getSizePixels(ImageSize size) {
        return switch (size) {
            case THUMBNAIL_150 -> 150;
            case THUMBNAIL_600 -> 600;
            default -> 300;
        };
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
}
