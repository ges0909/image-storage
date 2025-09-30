package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncImageServiceTest {

    @Mock
    private S3TransferManager transferManager;

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private ImageMetadataRepository metadataRepository;

    @Mock
    private Upload upload;

    private AsyncImageService asyncImageService;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket",
            "test-thumbnails",
            "test-kms-key",
            "https://cdn.example.com",
            10485760L,
            "eu-central-1",
            15,
            new int[]{150, 300},
            java.util.Set.of("image/jpeg", "image/png"),
            1000,
            "images"
        );
        asyncImageService = new AsyncImageService(transferManager, s3AsyncClient, metadataRepository, imageProperties);
    }

    @Test
    void uploadImage_ShouldCompleteSuccessfully() {
        // Arrange
        String imageId = "test-id";
        byte[] imageData = {1, 2, 3, 4};
        String contentType = "image/jpeg";
        ImageMetadata metadata = new ImageMetadata();

        when(transferManager.upload(any(UploadRequest.class))).thenReturn(upload);
        when(upload.completionFuture()).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = asyncImageService.uploadImage(imageId, imageData, contentType, metadata);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
    }
}
