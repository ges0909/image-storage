package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.exception.ImageNotFoundException;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.model.*;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Optimierter ImageService für große Dateien mit Database-Backend.
 */
@Service
public class OptimizedImageService implements ImageService {

    private static final Logger log = LoggerFactory.getLogger(OptimizedImageService.class);

    private final AsyncImageService asyncImageService;
    private final ImageMetadataRepository metadataRepository;
    private final ImageProperties imageProperties;

    public OptimizedImageService(AsyncImageService asyncImageService,
                                 ImageMetadataRepository metadataRepository,
                                 ImageProperties imageProperties) {
        this.asyncImageService = asyncImageService;
        this.metadataRepository = metadataRepository;
        this.imageProperties = imageProperties;
    }

    /**
     * Standard Upload-Methode - verwendet optimierte Implementierung.
     */
    public ImageResponse uploadImage(MultipartFile file, String title, String description, List<String> tags) {
        return uploadImageOptimized(file, title, description, tags);
    }

    /**
     * Optimierter Upload mit sofortiger Response und asynchroner Verarbeitung.
     */
    @Timed("image.upload.optimized")
    @Counted("image.upload.requests")
    public ImageResponse uploadImageOptimized(MultipartFile file, String title, String description, List<String> tags) {
        validateImageFile(file);

        String imageId = UUID.randomUUID().toString();

        try {
            byte[] imageData = file.getBytes();
            ImageDimensions dimensions = getImageDimensions(imageData);

            // Metadaten sofort in DB speichern
            ImageMetadata metadata = new ImageMetadata(
                imageId, title, description,
                tags != null ? Set.copyOf(tags) : Set.of(),
                file.getContentType(), file.getSize(),
                dimensions.width(), dimensions.height(),
                "images/" + imageId + "/original",
                getCurrentUser()
            );

            metadataRepository.save(metadata);

            // Asynchroner Upload startet im Hintergrund
            CompletableFuture<Void> uploadFuture = asyncImageService.uploadOriginalAsync(
                imageId, imageData, file.getContentType(), metadata);

            log.info("Image upload initiated: {} (async processing started)", imageId);

            return buildImageResponse(metadata);

        } catch (IOException e) {
            log.error("IO error during upload preparation for image: {}", imageId, e);
            throw new ImageProcessingException("Upload preparation failed", e);
        }
    }

    /**
     * Schnelle Metadaten-Abfrage aus Database mit Caching.
     */
    @Cacheable("image-metadata")
    @Timed("image.metadata.get")
    public ImageResponse getImageMetadata(String imageId) {
        ImageMetadata metadata = metadataRepository.findById(imageId)
            .orElseThrow(() -> new ImageNotFoundException("Image not found: " + imageId));

        return buildImageResponse(metadata);
    }

    /**
     * Optimierte Suche über Database-Indizes.
     */
    @Timed("image.search.optimized")
    public SearchResponse searchImagesOptimized(SearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page(),
            request.size(),
            Sort.by(Sort.Direction.fromString(request.sortDirection()), request.sortBy())
        );

        Page<ImageMetadata> page = metadataRepository.findBySearchCriteria(
            request.query(),
            request.contentType(),
            pageable
        );

        List<ImageResponse> images = page.getContent().stream()
            .map(this::buildImageResponse)
            .toList();

        return new SearchResponse(
            images,
            (int) page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }

    /**
     * Standard Stats - verwendet optimierte Database-Implementierung.
     */
    public ImageStats getImageStats() {
        return getImageStatsOptimized();
    }

    /**
     * Schnelle Statistiken aus Database.
     */
    @Cacheable("image-stats")
    @Timed("image.stats.get")
    public ImageStats getImageStatsOptimized() {
        Object[] stats = metadataRepository.getImageStatistics();

        if (stats != null && stats.length >= 3) {
            Long totalImages = (Long) stats[0];
            Long totalSize = (Long) stats[1];
            Double avgSize = (Double) stats[2];

            return new ImageStats(
                totalImages != null ? totalImages : 0L,
                totalSize != null ? totalSize : 0L,
                0L, // thumbnailCount - könnte separat berechnet werden
                0L, // totalDownloads - würde separates Tracking benötigen
                avgSize != null ? avgSize : 0.0
            );
        }

        return new ImageStats(0L, 0L, 0L, 0L, 0.0);
    }

    @Override
    public ImageResponse updateImageMetadata(String imageId, ImageUpdateRequest request) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteImage(String imageId) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String generateSignedUrl(String imageId, ImageSize size, Duration expiration) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getThumbnailUrl(String imageId, ImageSize size) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public SearchResponse searchImages(SearchRequest request) {
        return null;
    }

    @Override
    public List<ImageResponse> listImages(int page, int size) {
        SearchRequest request = new SearchRequest(null, null, null, page, size, "uploadDate", "desc");
        return searchImages(request).images();
    }

    @Override
    public List<ImageVersion> getImageVersions(String imageId) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ImageResponse restoreVersion(String imageId, String versionId) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void addTags(String imageId, List<String> tags) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void removeTags(String imageId, List<String> tags) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ImageAnalytics getImageAnalytics(String imageId) {
        return new ImageAnalytics(imageId, 0L, 0L, LocalDateTime.now(), List.of());
    }

    @Override
    public void batchDeleteImages(List<@Pattern(regexp = "[a-fA-F0-9-]{36}", message = "Invalid UUID format") String> imageIds) {
        // TODO: Implement with database backend
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (!isValidImageType(file.getContentType())) {
            throw new IllegalArgumentException("Invalid image type: " + file.getContentType());
        }
        if (file.getSize() > imageProperties.maxFileSize()) {
            throw new IllegalArgumentException("File too large: " + file.getSize());
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && imageProperties.supportedTypes().contains(contentType);
    }

    private ImageDimensions getImageDimensions(byte[] imageData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                throw new ImageProcessingException("Invalid image data");
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        }
    }

    private String getCurrentUser() {
        return "system"; // TODO: Integration mit Spring Security
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
            metadata.getUploadDate(),
            metadata.getLastModified(),
            metadata.getUploadedBy(),
            buildImageUrls(metadata.getImageId())
        );
    }

    private ImageUrls buildImageUrls(String imageId) {
        String baseUrl = imageProperties.cloudfrontDomain();
        return new ImageUrls(
            null, // Original requires signed URL
            baseUrl + "/images/" + imageId + "/thumbnail_150",
            baseUrl + "/images/" + imageId + "/thumbnail_300",
            baseUrl + "/images/" + imageId + "/thumbnail_600"
        );
    }
}
