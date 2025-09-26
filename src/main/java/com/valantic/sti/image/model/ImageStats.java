package com.valantic.sti.image.model;

public record ImageStats(
    long totalImages,
    long totalSizeBytes,
    long imagesThisMonth,
    long mostPopularTags,
    double averageImageSizeBytes
) {
}
