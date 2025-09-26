package com.valantic.sti.image.model;

import java.util.List;

public record SearchResponse(
    List<ImageResponse> images,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {
}
