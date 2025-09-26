package com.valantic.sti.image.model;

import java.time.LocalDateTime;

public record ImageVersion(
    String versionId,
    LocalDateTime createdDate,
    String createdBy,
    long sizeBytes,
    boolean isLatest
) {
}
