package com.valantic.sti.image.model;

import java.time.LocalDateTime;
import java.util.List;

public record ImageResponse(
    String id,
    String title,
    String description,
    List<String> tags,
    String contentType,
    long sizeBytes,
    ImageDimensions dimensions,
    LocalDateTime uploadDate,
    LocalDateTime lastModified,
    String uploadedBy,
    ImageUrls urls
) {
}
