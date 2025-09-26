package com.valantic.sti.image.model;

import java.time.LocalDateTime;
import java.util.List;

public record ImageAnalytics(
    String imageId,
    long downloadCount,
    long viewCount,
    LocalDateTime lastAccessed,
    List<String> accessLocations
) {
}
