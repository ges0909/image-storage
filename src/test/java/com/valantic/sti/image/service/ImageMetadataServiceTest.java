package com.valantic.sti.image.service;

import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.exception.ImageNotFoundException;
import com.valantic.sti.image.model.SearchRequest;
import com.valantic.sti.image.model.SearchResponse;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageMetadataServiceTest {

    @Mock
    private ImageMetadataRepository metadataRepository;

    @Mock
    private ImageUrlService imageUrlService;

    private ImageMetadataService imageMetadataService;

    @BeforeEach
    void setUp() {
        imageMetadataService = new ImageMetadataService(metadataRepository, imageUrlService);
    }

    @Test
    void findById_ShouldReturnMetadata_WhenImageExists() {
        // Arrange
        String imageId = "test-id";
        ImageMetadata metadata = createImageMetadata(imageId);
        when(metadataRepository.findById(imageId)).thenReturn(Optional.of(metadata));

        // Act
        ImageMetadata result = imageMetadataService.findById(imageId);

        // Assert
        assertThat(result).isEqualTo(metadata);
    }

    @Test
    void findById_ShouldThrowException_WhenImageNotFound() {
        // Arrange
        String imageId = "non-existent";
        when(metadataRepository.findById(imageId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> imageMetadataService.findById(imageId))
            .isInstanceOf(ImageNotFoundException.class)
            .hasMessageContaining("Image not found: " + imageId);
    }

    @Test
    void searchImages_ShouldReturnSearchResponse() {
        // Arrange
        SearchRequest request = new SearchRequest("test", null, null, 0, 10, "createdAt", "desc");
        ImageMetadata metadata = createImageMetadata("test-id");
        Page<ImageMetadata> page = new PageImpl<>(List.of(metadata));
        
        when(metadataRepository.findBySearchCriteria(anyString(), any(), any(Pageable.class)))
            .thenReturn(page);

        // Act
        SearchResponse result = imageMetadataService.searchImages(request);

        // Assert
        assertThat(result.images()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    private ImageMetadata createImageMetadata(String imageId) {
        ImageMetadata metadata = new ImageMetadata();
        metadata.setImageId(imageId);
        metadata.setTitle("Test Image");
        metadata.setDescription("Test Description");
        metadata.setTags(Set.of("tag1"));
        metadata.setContentType("image/jpeg");
        metadata.setFileSize(1024L);
        metadata.setWidth(800);
        metadata.setHeight(600);
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setUpdatedAt(LocalDateTime.now());
        metadata.setUploadedBy("test-user");
        return metadata;
    }
}