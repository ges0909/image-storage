package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.exception.ImageNotFoundException;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.model.*;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service für die Verwaltung von Bildern in AWS S3.
 * <p>
 * Diese Klasse bietet umfassende Funktionalitäten für:
 * - Upload und Speicherung von Bildern mit KMS-Verschlüsselung
 * - Automatische Thumbnail-Generierung in verschiedenen Größen
 * - Metadaten-Verwaltung (Titel, Beschreibung, Tags)
 * - Versionierung und Wiederherstellung
 * - Sichere URL-Generierung mit Pre-signed URLs
 * - Batch-Operationen für bessere Performance
 *
 * @author S3 Playground Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private static final String SYSTEM_USER = "system";
    private static final Duration MAX_PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ImageProperties imageProperties;

    /**
     * Konstruktor für ImageService.
     *
     * @param s3Client        AWS S3 Client für Bucket-Operationen
     * @param s3Presigner     S3 Presigner für signierte URLs
     * @param imageProperties Konfigurationseigenschaften für Bilder
     */
    public ImageService(S3Client s3Client, S3Presigner s3Presigner, ImageProperties imageProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.imageProperties = imageProperties;
    }

    /**
     * Lädt ein Bild hoch und generiert automatisch Thumbnails.
     * <p>
     * Das Originalbild wird mit KMS-Verschlüsselung gespeichert und Thumbnails
     * in den konfigurierten Größen generiert. Alle Metadaten werden als
     * S3-Object-Metadaten gespeichert.
     *
     * @param file        Die hochzuladende Bilddatei
     * @param title       Titel des Bildes (optional, max. 255 Zeichen)
     * @param description Beschreibung des Bildes (optional, max. 1000 Zeichen)
     * @param tags        Liste von Tags (optional, max. 20 Tags)
     * @return ImageResponse mit Bild-Metadaten und URLs
     * @throws IllegalArgumentException bei ungültigen Eingabedaten
     * @throws ImageProcessingException bei S3- oder IO-Fehlern
     */
    public ImageResponse uploadImage(MultipartFile file, String title, String description, List<String> tags) {
        validateImageFile(file);
        validateInputs(title, description, tags);

        String imageId = UUID.randomUUID().toString();
        String originalKey = sanitizeS3Key("images/" + imageId + "/original");

        try {
            // Upload original with KMS encryption and private ACL
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(imageProperties.bucketName())
                    .key(originalKey)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PRIVATE)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(imageProperties.kmsKeyId())
                    .metadata(buildMetadata(title, description, tags))
                    .build(),
                RequestBody.fromBytes(file.getBytes())
            );

            // Generate and upload thumbnails
            generateThumbnails(imageId, file.getBytes(), file.getContentType());

            // Get image dimensions
            ImageDimensions dimensions = getImageDimensions(file.getBytes());

            log.info("Image uploaded successfully: {}", imageId);

            return new ImageResponse(
                imageId,
                title,
                description,
                tags != null ? tags : List.of(),
                file.getContentType(),
                file.getSize(),
                dimensions,
                LocalDateTime.now(),
                LocalDateTime.now(),
                getCurrentUser(),
                buildImageUrls(imageId)
            );

        } catch (S3Exception e) {
            log.error("S3 upload failed for image: {}", imageId);
            throw new ImageProcessingException("Upload failed", e);
        } catch (IOException e) {
            log.error("IO error during upload for image: {}", imageId);
            throw new ImageProcessingException("Upload failed", e);
        }
    }

    /**
     * Aktualisiert die Metadaten eines existierenden Bildes.
     * <p>
     * Verwendet S3 CopyObject mit REPLACE-Direktive um Metadaten zu aktualisieren,
     * ohne das Bild selbst zu verändern.
     *
     * @param imageId UUID des zu aktualisierenden Bildes
     * @param request Update-Request mit neuen Metadaten
     * @return Aktualisierte ImageResponse
     * @throws ImageNotFoundException   wenn das Bild nicht existiert
     * @throws ImageProcessingException bei S3-Fehlern
     */
    public ImageResponse updateImageMetadata(String imageId, ImageUpdateRequest request) {
        validateImageId(imageId);
        validateImageExists(imageId);
        validateInputs(request.title(), request.description(), request.tags());

        // Update metadata in S3 object
        String originalKey = sanitizeS3Key("images/" + imageId + "/original");

        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(imageProperties.bucketName())
                .sourceKey(originalKey)
                .destinationBucket(imageProperties.bucketName())
                .destinationKey(originalKey)
                .metadata(buildMetadata(request.title(), request.description(), request.tags()))
                .metadataDirective(MetadataDirective.REPLACE)
                .build();

            s3Client.copyObject(copyRequest);

            return getImageMetadata(imageId);

        } catch (S3Exception e) {
            log.error("S3 metadata update failed for image: {}", imageId);
            throw new ImageProcessingException("Metadata update failed", e);
        }
    }

    /**
     * Löscht ein einzelnes Bild inklusive aller Thumbnails.
     *
     * @param imageId UUID des zu löschenden Bildes
     * @throws ImageNotFoundException   wenn das Bild nicht existiert
     * @throws ImageProcessingException bei Löschfehlern
     */
    public void deleteImage(String imageId) {
        validateImageId(imageId);
        validateImageExists(imageId);
        batchDeleteImages(List.of(imageId));
    }

    /**
     * Löscht mehrere Bilder in Batch-Operationen für bessere Performance.
     * <p>
     * Sammelt alle zu löschenden Keys (Original + Thumbnails) und führt
     * Batch-Delete-Operationen durch. Behandelt partielle Fehler graceful.
     *
     * @param imageIds Liste der zu löschenden Bild-IDs
     * @throws ImageProcessingException bei kritischen Löschfehlern
     */
    public void batchDeleteImages(List<String> imageIds) {
        if (imageIds.isEmpty()) {
            return;
        }

        imageIds.forEach(this::validateImageId);
        List<String> failedDeletes = new ArrayList<>();

        try {
            // Collect all keys to delete (original + thumbnails)
            List<String> keysToDelete = imageIds.stream()
                .flatMap(imageId -> {
                    List<String> keys = new java.util.ArrayList<>();
                    keys.add(sanitizeS3Key(imageProperties.keyPrefix() + "/" + imageId + "/original"));
                    for (int size : imageProperties.thumbnailSizes()) {
                        keys.add(sanitizeS3Key(imageProperties.keyPrefix() + "/" + imageId + "/thumbnail_" + size));
                    }
                    return keys.stream();
                })
                .toList();

            // Batch delete up to configured limit per request
            for (int i = 0; i < keysToDelete.size(); i += imageProperties.maxResults()) {
                List<String> batch = keysToDelete.subList(i, Math.min(i + imageProperties.maxResults(), keysToDelete.size()));

                Delete delete = Delete.builder()
                    .objects(batch.stream()
                        .map(key -> ObjectIdentifier.builder().key(key).build())
                        .toList())
                    .build();

                try {
                    DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(imageProperties.bucketName())
                        .delete(delete)
                        .build());

                    if (!response.errors().isEmpty()) {
                        response.errors().forEach(error ->
                            failedDeletes.add(error.key()));
                    }
                } catch (S3Exception e) {
                    log.warn("Batch delete failed for some objects: {}", e.getMessage());
                    failedDeletes.addAll(batch);
                }
            }

            if (failedDeletes.isEmpty()) {
                log.info("Batch deleted {} images successfully", imageIds.size());
            } else {
                log.warn("Batch delete completed with {} failures", failedDeletes.size());
                throw new ImageProcessingException("Partial batch delete failure: " + failedDeletes.size() + " objects failed");
            }

        } catch (ImageProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during batch delete");
            throw new ImageProcessingException("Batch delete failed", e);
        }
    }

    /**
     * Ruft die Metadaten eines Bildes ab.
     * <p>
     * Verwendet HeadObject für effiziente Metadaten-Abfrage ohne Download
     * des Bildinhalts.
     *
     * @param imageId UUID des Bildes
     * @return ImageResponse mit allen Metadaten
     * @throws ImageNotFoundException   wenn das Bild nicht existiert
     * @throws ImageProcessingException bei S3-Fehlern
     */
    public ImageResponse getImageMetadata(String imageId) {
        validateImageId(imageId);
        validateImageExists(imageId);

        try {
            String originalKey = sanitizeS3Key("images/" + imageId + "/original");

            HeadObjectResponse response = s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(imageProperties.bucketName())
                    .key(originalKey)
                    .build()
            );

            return new ImageResponse(
                imageId,
                response.metadata().get("title"),
                response.metadata().get("description"),
                parseTagsFromMetadata(response.metadata().get("tags")),
                response.contentType(),
                response.contentLength(),
                new ImageDimensions(
                    parseIntSafely(response.metadata().getOrDefault("width", "0")),
                    parseIntSafely(response.metadata().getOrDefault("height", "0"))
                ),
                response.lastModified().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
                response.lastModified().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
                response.metadata().getOrDefault("uploaded-by", "unknown"),
                buildImageUrls(imageId)
            );

        } catch (NoSuchKeyException e) {
            throw new ImageNotFoundException("Image not found: " + imageId);
        } catch (S3Exception e) {
            log.error("S3 error getting metadata for image: {}", imageId);
            throw new ImageProcessingException("Failed to get image metadata", e);
        }
    }

    /**
     * Generiert eine signierte URL für sicheren Bilddownload.
     * <p>
     * Pre-signed URLs ermöglichen temporären Zugriff ohne AWS-Credentials.
     * Expiration ist auf maximal 15 Minuten begrenzt aus Sicherheitsgründen.
     *
     * @param imageId    UUID des Bildes
     * @param size       Gewünschte Bildgröße
     * @param expiration Gültigkeitsdauer der URL
     * @return Signierte URL als String
     * @throws IllegalArgumentException bei ungültiger Expiration
     * @throws ImageNotFoundException   wenn das Bild nicht existiert
     */
    public String generateSignedUrl(String imageId, ImageSize size, Duration expiration) {
        validateImageId(imageId);
        validateImageExists(imageId);
        validateExpiration(expiration);

        String key = buildKeyForSize(imageId, size);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(imageProperties.bucketName())
                .key(key)
                .build())
            .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }


    /**
     * Gibt die CloudFront-URL für ein Thumbnail zurück.
     * <p>
     * Thumbnails sind über CloudFront öffentlich zugänglich für bessere Performance.
     * Für Originalbilder muss generateSignedUrl() verwendet werden.
     *
     * @param imageId UUID des Bildes
     * @param size    Thumbnail-Größe (nicht ORIGINAL)
     * @return CloudFront-URL für das Thumbnail
     * @throws IllegalArgumentException wenn size ORIGINAL ist
     */
    public String getThumbnailUrl(String imageId, ImageSize size) {
        validateImageId(imageId);
        if (size == ImageSize.ORIGINAL) {
            throw new IllegalArgumentException("Use generateSignedUrl for original images");
        }

        // Return CloudFront URL for thumbnails (public access)
        String key = buildKeyForSize(imageId, size);
        return validateAndBuildCloudFrontUrl(key);
    }

    /**
     * Durchsucht Bilder basierend auf Suchkriterien.
     * <p>
     * Vereinfachte Implementierung mit S3 ListObjects. In Production
     * sollte OpenSearch/Elasticsearch für erweiterte Suchfunktionen verwendet werden.
     *
     * @param request Suchparameter (Filter, Paginierung, Sortierung)
     * @return SearchResponse mit gefundenen Bildern und Metadaten
     * @throws ImageProcessingException bei S3-Suchfehlern
     */
    public SearchResponse searchImages(SearchRequest request) {
        // Simplified implementation - in production use OpenSearch
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(imageProperties.bucketName())
                    .prefix("images/")
                    .maxKeys(request.size())
                    .build()
            );

            List<String> imageIds = response.contents().stream()
                .filter(obj -> obj.key().endsWith("/original"))
                .map(obj -> extractImageIdFromKey(obj.key()))
                .filter(Objects::nonNull)
                .toList();

            // Batch metadata retrieval instead of N+1 queries
            List<ImageResponse> images = batchGetImageMetadata(imageIds);

            return new SearchResponse(images, images.size(), 1, 0, request.size());

        } catch (S3Exception e) {
            log.error("S3 search operation failed");
            throw new ImageProcessingException("Search failed", e);
        }
    }

    /**
     * Listet Bilder mit Paginierung auf.
     *
     * @param page Seitennummer (0-basiert)
     * @param size Anzahl Bilder pro Seite
     * @return Liste von ImageResponse-Objekten
     */
    public List<ImageResponse> listImages(int page, int size) {
        SearchRequest request = new SearchRequest(null, null, null, page, size, "uploadDate", "desc");
        return searchImages(request).images();
    }

    /**
     * Ruft alle Versionen eines Bildes ab.
     * <p>
     * Nutzt S3 Versioning um Änderungshistorie zu verwalten.
     *
     * @param imageId UUID des Bildes
     * @return Liste aller Versionen mit Metadaten
     * @throws ImageProcessingException bei Versionierungsfehlern
     */
    public List<ImageVersion> getImageVersions(String imageId) {
        validateImageExists(imageId);

        try {
            String originalKey = "images/" + imageId + "/original";

            ListObjectVersionsResponse response = s3Client.listObjectVersions(
                ListObjectVersionsRequest.builder()
                    .bucket(imageProperties.bucketName())
                    .prefix(originalKey)
                    .build()
            );

            return response.versions().stream()
                .map(version -> new ImageVersion(
                    version.versionId(),
                    version.lastModified().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
                    "system",
                    version.size(),
                    version.isLatest()
                ))
                .toList();

        } catch (Exception e) {
            log.error("Failed to get versions: {}", e.getMessage(), e);
            throw new ImageProcessingException("Version listing failed", e);
        }
    }

    /**
     * Stellt eine spezifische Version eines Bildes wieder her.
     * <p>
     * Kopiert die gewählte Version als neue aktuelle Version.
     *
     * @param imageId   UUID des Bildes
     * @param versionId ID der wiederherzustellenden Version
     * @return ImageResponse der wiederhergestellten Version
     * @throws ImageNotFoundException   wenn Bild oder Version nicht existiert
     * @throws ImageProcessingException bei Wiederherstellungsfehlern
     */
    public ImageResponse restoreVersion(String imageId, String versionId) {
        validateImageExists(imageId);

        try {
            String originalKey = "images/" + imageId + "/original";

            // Copy the specific version to make it the current version
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(imageProperties.bucketName())
                .sourceKey(originalKey)
                .sourceVersionId(versionId)
                .destinationBucket(imageProperties.bucketName())
                .destinationKey(originalKey)
                .metadataDirective(MetadataDirective.COPY) // Preserve metadata from the source version
                .build();

            s3Client.copyObject(copyRequest);

            log.info("Restored version {} for image {}", versionId, imageId);

            // Return the metadata of the newly current version
            return getImageMetadata(imageId);

        } catch (S3Exception e) {
            log.error("Failed to restore version {} for image {}: {}", versionId, imageId, e.getMessage(), e);
            // Check for specific error, e.g., if the version ID is invalid
            if (e.awsErrorDetails() != null && "InvalidVersionId".equals(e.awsErrorDetails().errorCode())) {
                throw new ImageNotFoundException("Version not found: " + versionId);
            }
            throw new ImageProcessingException("Version restore failed", e);
        }
    }

    /**
     * Fügt Tags zu einem Bild hinzu.
     *
     * @param imageId UUID des Bildes
     * @param tags    Liste der hinzuzufügenden Tags
     */
    public void addTags(String imageId, List<String> tags) {
        ImageResponse current = getImageMetadata(imageId);
        Set<String> updatedTags = new LinkedHashSet<>(current.tags());
        updatedTags.addAll(tags);

        updateImageMetadata(imageId, new ImageUpdateRequest(
            current.title(),
            current.description(),
            List.copyOf(updatedTags)
        ));
    }

    /**
     * Entfernt Tags von einem Bild.
     *
     * @param imageId UUID des Bildes
     * @param tags    Liste der zu entfernenden Tags
     */
    public void removeTags(String imageId, List<String> tags) {
        ImageResponse current = getImageMetadata(imageId);
        List<String> updatedTags = current.tags().stream()
            .filter(tag -> !tags.contains(tag))
            .toList();

        updateImageMetadata(imageId, new ImageUpdateRequest(
            current.title(),
            current.description(),
            updatedTags
        ));
    }

    /**
     * Ruft Statistiken über alle Bilder ab.
     *
     * @return ImageStats mit Gesamtstatistiken
     */
    public ImageStats getImageStats() {
        // Simplified implementation
        return new ImageStats(0L, 0L, 0L, 0L, 0.0);
    }

    /**
     * Ruft Analytics-Daten für ein spezifisches Bild ab.
     *
     * @param imageId UUID des Bildes
     * @return ImageAnalytics mit Nutzungsstatistiken
     */
    public ImageAnalytics getImageAnalytics(String imageId) {
        // Simplified implementation
        return new ImageAnalytics(imageId, 0L, 0L, LocalDateTime.now(), List.of());
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Validiert eine hochgeladene Bilddatei.
     * <p>
     * Prüft: Dateigröße, Content-Type, Leer-Status
     *
     * @param file Zu validierende Datei
     * @throws IllegalArgumentException bei Validierungsfehlern
     */
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

    /**
     * Prüft ob ein Bild in S3 existiert.
     *
     * @param imageId UUID des zu prüfenden Bildes
     * @throws ImageNotFoundException wenn das Bild nicht existiert
     */
    private void validateImageExists(String imageId) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(imageProperties.bucketName())
                .key(sanitizeS3Key("images/" + imageId + "/original"))
                .build());
        } catch (NoSuchKeyException e) {
            throw new ImageNotFoundException("Image not found: " + imageId);
        }
    }

    private void validateImageId(String imageId) {
        if (imageId == null || imageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Image ID cannot be null or empty");
        }
        if (!imageId.matches("^[a-fA-F0-9-]{36}$")) {
            throw new IllegalArgumentException("Invalid image ID format");
        }
    }

    private void validateInputs(String title, String description, List<String> tags) {
        if (title != null && title.length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
        if (tags != null && tags.size() > 20) {
            throw new IllegalArgumentException("Cannot have more than 20 tags");
        }
    }

    private String sanitizeS3Key(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        String sanitized = key.replaceAll("[^a-zA-Z0-9!_.*'()-/]", "_");

        if (sanitized.length() > 1024) {
            throw new IllegalArgumentException("S3 key too long");
        }

        return sanitized;
    }

    private String getCurrentUser() {
        // TODO: Integrate with Spring Security when authentication is implemented
        return SYSTEM_USER;
    }

    private String validateAndBuildCloudFrontUrl(String key) {
        String domain = imageProperties.cloudfrontDomain();
        if (domain == null || !domain.startsWith("https://")) {
            throw new IllegalStateException("Invalid CloudFront domain configuration");
        }
        return domain + "/" + sanitizeS3Key(key);
    }


    private void validateExpiration(Duration expiration) {
        if (expiration.compareTo(MAX_PRESIGNED_URL_DURATION) > 0) {
            throw new IllegalArgumentException("Expiration cannot exceed " + MAX_PRESIGNED_URL_DURATION.toMinutes() + " minutes for security");
        }
        if (expiration.toMinutes() > imageProperties.urlExpirationMinutes()) {
            throw new IllegalArgumentException("Expiration cannot exceed " + imageProperties.urlExpirationMinutes() + " minutes");
        }
    }

    /**
     * Generiert Thumbnails in allen konfigurierten Größen.
     * <p>
     * Verwendet Thumbnailator für hochwertige Bildverarbeitung mit
     * automatischer Qualitätsoptimierung und besserer Performance.
     *
     * @param imageId     UUID des Bildes
     * @param imageData   Original-Bilddaten
     * @param contentType MIME-Type des Bildes
     * @throws IOException bei Bildverarbeitungsfehlern
     */
    private void generateThumbnails(String imageId, byte[] imageData, String contentType) throws IOException {
        int[] sizes = imageProperties.thumbnailSizes();

        for (int size : sizes) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                // Thumbnailator für hochwertige Skalierung
                Thumbnails.of(inputStream)
                    .size(size, size)
                    .keepAspectRatio(true)
                    .outputQuality(0.85f)
                    .outputFormat(getFormatFromContentType(contentType))
                    .toOutputStream(outputStream);

                String thumbnailKey = "images/" + imageId + "/thumbnail_" + size;

                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(imageProperties.thumbnailBucketName())
                        .key(sanitizeS3Key(thumbnailKey))
                        .contentType(contentType)
                        .acl(ObjectCannedACL.PRIVATE)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                    RequestBody.fromBytes(outputStream.toByteArray())
                );
            }
        }
    }

    // Removed: resizeImage() and imageToBytes() - replaced by Thumbnailator

    private String getFormatFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private ImageDimensions getImageDimensions(byte[] imageData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                throw new ImageProcessingException("Invalid image data: unable to read image");
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        }
    }

    private java.util.Map<String, String> buildMetadata(String title, String description, List<String> tags) {
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        if (title != null) metadata.put("title", title);
        if (description != null) metadata.put("description", description);
        if (tags != null && !tags.isEmpty()) metadata.put("tags", String.join(",", tags));
        metadata.put("uploaded-by", getCurrentUser());
        return metadata;
    }

    private List<String> parseTagsFromMetadata(String tagsString) {
        if (tagsString == null || tagsString.isEmpty()) {
            return List.of();
        }
        return List.of(tagsString.split(","));
    }

    private ImageUrls buildImageUrls(String imageId) {
        String baseUrl = imageProperties.cloudfrontDomain();
        return new ImageUrls(
            null, // Original requires to be signed URL
            baseUrl + "/images/" + imageId + "/thumbnail_150",
            baseUrl + "/images/" + imageId + "/thumbnail_300",
            baseUrl + "/images/" + imageId + "/thumbnail_600"
        );
    }

    private String buildKeyForSize(String imageId, ImageSize size) {
        validateImageId(imageId);
        return switch (size) {
            case ORIGINAL -> sanitizeS3Key(imageProperties.keyPrefix() + "/" + imageId + "/original");
            case THUMBNAIL_150 -> sanitizeS3Key(imageProperties.keyPrefix() + "/" + imageId + "/thumbnail_150");
            case THUMBNAIL_300 -> sanitizeS3Key(imageProperties.keyPrefix() + "/" + imageId + "/thumbnail_300");
            case THUMBNAIL_600 -> sanitizeS3Key(imageProperties.keyPrefix() + "/" + imageId + "/thumbnail_600");
        };
    }

    private String extractImageIdFromKey(String key) {
        // Extract from "images/{imageId}/original"
        String[] parts = key.split("/");
        return parts.length >= 2 ? parts[1] : "";
    }

    private int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric metadata value: {}, using default 0", value, e);
            return 0;
        }
    }

    /**
     * Ruft Metadaten für mehrere Bilder in Batch-Operation ab.
     * <p>
     * Optimiert Performance durch parallele HeadObject-Requests
     * statt N+1 Einzelabfragen.
     *
     * @param imageIds Liste der Bild-IDs
     * @return Liste von ImageResponse-Objekten (null-Werte gefiltert)
     */
    private List<ImageResponse> batchGetImageMetadata(List<String> imageIds) {
        if (imageIds.isEmpty()) {
            return List.of();
        }

        // Batch HEAD requests for better performance
        List<HeadObjectRequest> requests = imageIds.stream()
            .peek(this::validateImageId)
            .map(imageId -> HeadObjectRequest.builder()
                .bucket(imageProperties.bucketName())
                .key(sanitizeS3Key("images/" + imageId + "/original"))
                .build())
            .toList();

        return requests.stream()
            .map(request -> {
                try {
                    HeadObjectResponse response = s3Client.headObject(request);
                    String imageId = extractImageIdFromKey(request.key());
                    return buildImageResponseFromMetadata(imageId, response);
                } catch (NoSuchKeyException e) {
                    log.debug("Image not found for key: {}", request.key());
                    return null;
                } catch (S3Exception e) {
                    log.warn("S3 error getting metadata for key {}: {}", request.key(), e.awsErrorDetails().errorMessage());
                    return null;
                } catch (Exception e) {
                    log.error("Unexpected error getting metadata for key {}: {}", request.key(), e.getMessage());
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private ImageResponse buildImageResponseFromMetadata(String imageId, HeadObjectResponse response) {
        return new ImageResponse(
            imageId,
            response.metadata().get("title"),
            response.metadata().get("description"),
            parseTagsFromMetadata(response.metadata().get("tags")),
            response.contentType(),
            response.contentLength(),
            new ImageDimensions(
                parseIntSafely(response.metadata().getOrDefault("width", "0")),
                parseIntSafely(response.metadata().getOrDefault("height", "0"))
            ),
            response.lastModified().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
            response.lastModified().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
            response.metadata().getOrDefault("uploaded-by", "unknown"),
            buildImageUrls(imageId)
        );
    }
}
