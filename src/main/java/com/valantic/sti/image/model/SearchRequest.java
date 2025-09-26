package com.valantic.sti.image.model;

import java.util.List;

public record SearchRequest(
    String query,
    List<String> tags,
    String contentType,
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
}
