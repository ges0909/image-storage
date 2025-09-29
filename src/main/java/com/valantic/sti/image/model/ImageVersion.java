package com.valantic.sti.image.model;

import java.time.LocalDateTime;

public record ImageVersion(
    String versionId,
    String imageId,
    LocalDateTime createdAt,
    long sizeBytes,
    boolean isLatest,
    String etag
) {
}