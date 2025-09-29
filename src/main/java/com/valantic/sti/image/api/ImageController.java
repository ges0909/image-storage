package com.valantic.sti.image.api;

import com.valantic.sti.image.model.*;
import com.valantic.sti.image.service.ImageService;
import com.valantic.sti.image.validation.ValidImageFile;
import com.valantic.sti.image.validation.ValidUUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Storage", description = "🖼️ Secure image storage and management API")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // 📤 Upload & Management

    /**
     * Uploads a new image with optional metadata and tags.
     *
     * @param file        the image file to upload (JPEG, PNG, GIF supported)
     * @param title       optional image title
     * @param description optional image description
     * @param tags        optional list of tags for categorization
     * @return created image metadata with generated ID and S3 location
     */
    @Operation(summary = "Upload Image", description = "Upload a new image with optional metadata")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Image uploaded successfully",
            content = @Content(schema = @Schema(implementation = ImageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
        @ApiResponse(responseCode = "413", description = "File too large")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> uploadImage(
        @Parameter(description = "Image file to upload", required = true)
        @RequestParam("file") @ValidImageFile MultipartFile file,
        @Parameter(description = "Image title")
        @RequestParam(required = false) String title,
        @Parameter(description = "Image description")
        @RequestParam(required = false) String description,
        @Parameter(description = "Image tags")
        @RequestParam(required = false) List<String> tags) {

        ImageResponse response = imageService.uploadImage(file, title, description, tags);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/images/" + response.id()))
            .body(response);
    }

    /**
     * Updates image metadata (title, description, tags).
     *
     * @param imageId the UUID of the image to update
     * @param request the update request containing new metadata
     * @return updated image metadata
     */
    @PutMapping("/{imageId}")
    public ResponseEntity<ImageResponse> updateImage(
        @PathVariable @ValidUUID String imageId,
        @RequestBody @Valid ImageUpdateRequest request) {

        ImageResponse response = imageService.updateImageMetadata(imageId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently deletes an image and all its versions from S3.
     *
     * @param imageId the UUID of the image to delete
     * @return empty response with 204 No Content status
     */
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN') or @imageService.isOwner(#imageId, authentication.name)")
    public ResponseEntity<Void> deleteImage(@PathVariable @ValidUUID String imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    // 📥 Download & Access

    /**
     * Retrieves image metadata including upload date, size, and tags.
     *
     * @param imageId the UUID of the image
     * @return image metadata without the actual file content
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> getImageMetadata(@PathVariable @ValidUUID String imageId) {
        ImageResponse response = imageService.getImageMetadata(imageId);
        return ResponseEntity.ok(response);
    }

    /**
     * Generates a time-limited signed URL for secure image download.
     *
     * @param imageId           the UUID of the image
     * @param size              the desired image size (ORIGINAL, LARGE, MEDIUM, SMALL, THUMBNAIL)
     * @param expirationMinutes URL validity period (1-15 minutes, default 15)
     * @return signed download URL with expiration info
     */
    @Operation(summary = "Get Download URL", description = "Generate signed URL for image download")
    @GetMapping("/{imageId}/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
        @Parameter(description = "Image ID", required = true)
        @PathVariable @ValidUUID String imageId,
        @Parameter(description = "Image size")
        @RequestParam(defaultValue = "ORIGINAL") ImageSize size,
        @Parameter(description = "URL expiration in minutes (max 15)")
        @RequestParam(defaultValue = "15") @Min(1) @Max(15) int expirationMinutes) {

        String signedUrl = imageService.generateSignedUrl(imageId, size, Duration.ofMinutes(expirationMinutes));
        return ResponseEntity.ok(Map.of("downloadUrl", signedUrl, "expiresIn", expirationMinutes + " minutes"));
    }

    /**
     * Gets a public thumbnail URL for the specified image size.
     *
     * @param imageId the UUID of the image
     * @param size    the thumbnail size (SMALL, MEDIUM, LARGE)
     * @return public thumbnail URL (no expiration)
     */
    @GetMapping("/{imageId}/thumbnails/{size}")
    public ResponseEntity<Map<String, String>> getThumbnailUrl(
        @PathVariable @ValidUUID String imageId,
        @PathVariable ImageSize size) {

        String thumbnailUrl = imageService.getThumbnailUrl(imageId, size);
        return ResponseEntity.ok(Map.of("thumbnailUrl", thumbnailUrl));
    }

    // 🔍 Search & Discovery

    /**
     * Searches images with advanced filtering and pagination support.
     *
     * @param query         text search in title and description
     * @param tags          filter by specific tags
     * @param contentType   filter by MIME type (image/jpeg, image/png, etc.)
     * @param page          page number (0-based)
     * @param size          page size (1-100, default 20)
     * @param sortBy        sort field (uploadDate, title, size)
     * @param sortDirection sort order (asc, desc)
     * @return paginated search results with metadata
     */
    @Operation(summary = "Search Images", description = "Search images with filters and pagination")
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchImages(
        @Parameter(description = "Search query")
        @RequestParam(required = false) String query,
        @Parameter(description = "Filter by tags")
        @RequestParam(required = false) List<String> tags,
        @Parameter(description = "Filter by content type")
        @RequestParam(required = false) String contentType,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "uploadDate") @Pattern(regexp = "uploadDate|title|size") String sortBy,
        @Parameter(description = "Sort direction")
        @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String sortDirection) {

        SearchRequest searchRequest = new SearchRequest(query, tags, contentType, page, size, sortBy, sortDirection);
        SearchResponse response = imageService.searchImages(searchRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all images with basic pagination.
     *
     * @param page page number (0-based, default 0)
     * @param size page size (default 20)
     * @return list of image metadata
     */
    @GetMapping
    public ResponseEntity<List<ImageResponse>> listImages(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        List<ImageResponse> images = imageService.listImages(page, size);
        return ResponseEntity.ok(images);
    }

    // 📚 Version Management

    /**
     * Retrieves all versions of an image (S3 versioning).
     *
     * @param imageId the UUID of the image
     * @return list of all image versions with timestamps
     */
    @GetMapping("/{imageId}/versions")
    public ResponseEntity<List<ImageVersion>> getImageVersions(@PathVariable @ValidUUID String imageId) {
        List<ImageVersion> versions = imageService.getImageVersions(imageId);
        return ResponseEntity.ok(versions);
    }

    /**
     * Restores a specific version of an image as the current version.
     *
     * @param imageId   the UUID of the image
     * @param versionId the UUID of the version to restore
     * @return updated image metadata after restoration
     */
    @PostMapping("/{imageId}/versions/{versionId}/restore")
    public ResponseEntity<ImageResponse> restoreVersion(
        @PathVariable @ValidUUID String imageId,
        @PathVariable @ValidUUID String versionId) {

        ImageResponse response = imageService.restoreVersion(imageId, versionId);
        return ResponseEntity.ok(response);
    }

    // 🏷️ Tag Management

    /**
     * Adds new tags to an existing image.
     *
     * @param imageId the UUID of the image
     * @param tags    list of tags to add
     * @return empty response with 200 OK status
     */
    @PostMapping("/{imageId}/tags")
    public ResponseEntity<Void> addTags(
        @PathVariable @ValidUUID String imageId,
        @RequestBody List<String> tags) {

        imageService.addTags(imageId, tags);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes specific tags from an image.
     *
     * @param imageId the UUID of the image
     * @param tags    list of tags to remove
     * @return empty response with 200 OK status
     */
    @DeleteMapping("/{imageId}/tags")
    public ResponseEntity<Void> removeTags(
        @PathVariable @ValidUUID String imageId,
        @RequestParam List<String> tags) {

        imageService.removeTags(imageId, tags);
        return ResponseEntity.ok().build();
    }

    // 📊 Analytics & Stats

    /**
     * Retrieves global image storage statistics.
     *
     * @return overall stats including total images, storage usage, and popular tags
     */
    @GetMapping("/stats")
    public ResponseEntity<ImageStats> getImageStats() {
        ImageStats stats = imageService.getImageStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves analytics data for a specific image.
     *
     * @param imageId the UUID of the image
     * @return analytics including download count, view history, and access patterns
     */
    @GetMapping("/{imageId}/analytics")
    public ResponseEntity<ImageAnalytics> getImageAnalytics(@PathVariable @ValidUUID String imageId) {
        ImageAnalytics analytics = imageService.getImageAnalytics(imageId);
        return ResponseEntity.ok(analytics);
    }
}
