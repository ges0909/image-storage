package com.valantic.sti.image.model;

import java.util.List;

public record ImageUpdateRequest(
    String title,
    String description,
    List<String> tags
) {
}
