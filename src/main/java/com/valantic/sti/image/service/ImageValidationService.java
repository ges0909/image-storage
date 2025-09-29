package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ImageValidationService {

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_TAGS_COUNT = 20;

    private final ImageProperties imageProperties;
    private final ImageProcessingService imageProcessingService;

    public ImageValidationService(ImageProperties imageProperties, ImageProcessingService imageProcessingService) {
        this.imageProperties = imageProperties;
        this.imageProcessingService = imageProcessingService;
    }

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > imageProperties.maxFileSize()) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + imageProperties.maxFileSize());
        }

        String contentType = file.getContentType();
        if (!imageProcessingService.isValidImageFormat(contentType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }

        String filename = file.getOriginalFilename();
        if (filename != null && !SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }
    }

    public void validateInputs(String title, String description, List<String> tags) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title exceeds maximum length: " + MAX_TITLE_LENGTH);
        }

        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description exceeds maximum length: " + MAX_DESCRIPTION_LENGTH);
        }

        if (tags != null && tags.size() > MAX_TAGS_COUNT) {
            throw new IllegalArgumentException("Too many tags. Maximum allowed: " + MAX_TAGS_COUNT);
        }

        if (tags != null) {
            for (String tag : tags) {
                if (tag == null || tag.trim().isEmpty()) {
                    throw new IllegalArgumentException("Tags cannot be null or empty");
                }
                if (tag.length() > 50) {
                    throw new IllegalArgumentException("Tag exceeds maximum length: 50");
                }
            }
        }
    }

    public String sanitizeS3Key(String key) {
        return key.replaceAll("[^a-zA-Z0-9._/-]", "_");
    }
}
