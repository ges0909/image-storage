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
 * Zentrale Orchestrierungsschicht für alle bildverarbeitungsbezogenen Operationen.
 * <p>
 * Diese Service-Klasse koordiniert die Zusammenarbeit zwischen spezialisierten Services:
 * - S3StorageService: AWS S3 Upload/Download
 * - ImageProcessingService: Thumbnail-Generierung und Bildverarbeitung
 * - ImageMetadataService: Datenbankoperationen für Metadaten
 * - ImageUrlService: Signierte URL-Generierung
 * - ImageValidationService: Input-Validierung und Sanitization
 * - AsyncImageService: Asynchrone Hintergrundverarbeitung
 * <p>
 * Performance-Features:
 * - Async Upload für große Dateien (10-100 MB)
 * - Micrometer-Metriken für Monitoring
 * - Transaktionale Sicherheit
 * - Caching über ImageMetadataService
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
     * 📤 Synchroner Bild-Upload mit vollständiger Verarbeitung.
     * ⚠️ Für große Dateien (>10 MB) uploadImageAsync() verwenden!
     */
    @Timed("image.upload.sync")
    @Counted("image.upload.requests")
    public ImageResponse uploadImage(MultipartFile file, String title, String description, List<String> tags) {
        return imageUploadService.uploadSync(file, title, description, tags);
    }

    /**
     * ⚡ Asynchroner Bild-Upload für optimale Performance bei großen Dateien.
     */
    @Timed("image.upload.async")
    @Counted("image.upload.async.requests")
    public ImageResponse uploadImageAsync(MultipartFile file, String title, String description, List<String> tags) {
        return imageUploadService.uploadAsync(file, title, description, tags);
    }

    /**
     * 📊 Metadaten-Abruf mit Redis-Caching (1h TTL).
     */
    public ImageResponse getImageMetadata(String imageId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        return buildImageResponse(metadata);
    }

    /**
     * ✏️ Metadaten-Update mit partieller Aktualisierung.
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
     * 🔍 Datenbankbasierte Suche, deutlich schneller als S3 ListObjects.
     */
    public SearchResponse searchImages(SearchRequest request) {
        return imageMetadataService.searchImages(request);
    }

    /**
     * 🔗 Generierung signierter URLs für sicheren, zeitlich begrenzten Zugriff.
     */
    @Timed("image.url.generate")
    public String generateSignedUrl(String imageId, ImageSize size, Duration expiration) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        String key = size == ImageSize.ORIGINAL ? metadata.getS3Key() :
            "thumbnails/" + imageId + "/" + getSizePixels(size) + ".webp";
        String bucketName = size == ImageSize.ORIGINAL ?
            imageProperties.bucketName() : imageProperties.thumbnailBucketName();
        return imageUrlService.generatePresignedUrl(bucketName, key, (int) expiration.toMinutes());
    }

    /**
     * 🖼️ Thumbnail-URL mit Standard-Gültigkeit (15min).
     */
    public String getThumbnailUrl(String imageId, ImageSize size) {
        if (size == ImageSize.ORIGINAL) {
            throw new IllegalArgumentException("Use generateSignedUrl for original images");
        }
        String key = "thumbnails/" + imageId + "/" + getSizePixels(size) + ".webp";
        return imageUrlService.generatePresignedUrl(imageProperties.thumbnailBucketName(), key);
    }

    /**
     * 📋 Paginierte Bildliste, sortiert nach Erstellungsdatum.
     */
    @Timed("image.list")
    public List<ImageResponse> listImages(int page, int size) {
        SearchRequest request = new SearchRequest(null, null, null, page, size, "createdAt", "desc");
        return searchImages(request).images();
    }

    @Timed("image.versions.get")
    public List<ImageVersion> getImageVersions(String imageId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        return s3StorageService.getObjectVersions(metadata.getS3Key());
    }

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

    @Timed("image.stats.get")
    public ImageStats getImageStats() {
        // TODO: Implement proper statistics calculation from database
        return new ImageStats(
            0L, 0L, Map.of(), Map.of(), 0L
        );
    }

    @Timed("image.analytics.get")
    public ImageAnalytics getImageAnalytics(String imageId) {
        imageMetadataService.findById(imageId);
        // TODO: Implement proper analytics tracking
        return new ImageAnalytics(
            imageId, 0L, 0L, null, List.of()
        );
    }

    /**
     * 🗑️ Vollständige Bildlöschung: S3-Objekte + Metadaten.
     */
    @Transactional
    public void deleteImage(String imageId) {
        ImageMetadata metadata = imageMetadataService.findById(imageId);
        s3StorageService.deleteImage(metadata.getS3Key());
        imageMetadataService.deleteById(imageId);
        log.info("Image deleted: {}", imageId);
    }

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

    /**
     * 🏗️ ImageResponse-Builder mit URL-Generierung.
     */
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
