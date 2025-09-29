package com.valantic.sti.image.model;

import java.util.Map;

public record ImageStats(
    long totalImages,
    long totalSizeBytes,
    Map<String, Long> imagesByContentType,
    Map<String, Long> imagesByTag,
    long averageSizeBytes
) {
}