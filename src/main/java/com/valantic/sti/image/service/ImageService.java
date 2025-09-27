package com.valantic.sti.image.service;

import com.valantic.sti.image.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * Interface for image service implementations.
 */
public interface ImageService {

    ImageResponse uploadImage(MultipartFile file, String title, String description, List<String> tags);

    ImageResponse updateImageMetadata(String imageId, ImageUpdateRequest request);

    void deleteImage(String imageId);

    ImageResponse getImageMetadata(String imageId);

    String generateSignedUrl(String imageId, ImageSize size, Duration expiration);

    String getThumbnailUrl(String imageId, ImageSize size);

    SearchResponse searchImages(SearchRequest request);

    List<ImageResponse> listImages(int page, int size);

    List<ImageVersion> getImageVersions(String imageId);

    ImageResponse restoreVersion(String imageId, String versionId);

    void addTags(String imageId, List<String> tags);

    void removeTags(String imageId, List<String> tags);

    ImageStats getImageStats();

    ImageAnalytics getImageAnalytics(String imageId);

    void batchDeleteImages(@Valid @Size(min = 1, max = 100) List<@Pattern(regexp = "[a-fA-F0-9-]{36}", message = "Invalid UUID format") String> imageIds);
}
