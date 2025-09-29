package com.valantic.sti.image.service;

import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.exception.ImageNotFoundException;
import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.SearchRequest;
import com.valantic.sti.image.model.SearchResponse;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import io.micrometer.core.annotation.Timed;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageMetadataService {

    private final ImageMetadataRepository metadataRepository;
    private final ImageUrlService imageUrlService;

    public ImageMetadataService(ImageMetadataRepository metadataRepository, ImageUrlService imageUrlService) {
        this.metadataRepository = metadataRepository;
        this.imageUrlService = imageUrlService;
    }

    public ImageMetadata save(ImageMetadata metadata) {
        return metadataRepository.save(metadata);
    }

    @Cacheable("image-metadata")
    @Timed("image.metadata.get")
    public ImageMetadata findById(String imageId) {
        return metadataRepository.findById(imageId)
            .orElseThrow(() -> new ImageNotFoundException("Image not found: " + imageId));
    }

    @Timed("image.search.optimized")
    public SearchResponse searchImages(SearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page(),
            request.size(),
            Sort.by(Sort.Direction.fromString(request.sortDirection()), request.sortBy())
        );

        Page<ImageMetadata> page = metadataRepository.findBySearchCriteria(
            request.query(),
            request.contentType(),
            pageable
        );

        List<ImageResponse> images = page.getContent().stream()
            .map(this::buildImageResponse)
            .toList();

        return new SearchResponse(
            images,
            (int) page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }

    public void deleteById(String imageId) {
        metadataRepository.deleteById(imageId);
    }

    private ImageResponse buildImageResponse(ImageMetadata metadata) {
        return new ImageResponse(
            metadata.getImageId(),
            metadata.getTitle(),
            metadata.getDescription(),
            List.copyOf(metadata.getTags()),
            metadata.getContentType(),
            metadata.getFileSize(),
            new com.valantic.sti.image.model.ImageDimensions(metadata.getWidth(), metadata.getHeight()),
            metadata.getCreatedAt(),
            metadata.getUpdatedAt(),
            metadata.getUploadedBy(),
            imageUrlService.buildImageUrls(metadata.getImageId())
        );
    }
}