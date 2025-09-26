package com.valantic.sti.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void uploadFile_shouldCallPutObject() {
        // Arrange
        String bucketName = "test-bucket";
        String key = "test-key";
        byte[] content = "test content".getBytes();

        // Act & Assert
        assertDoesNotThrow(() -> s3Service.uploadFile(bucketName, key, content));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFileWithMetadata_shouldCallPutObjectWithMetadata() {
        // Arrange
        String bucketName = "test-bucket";
        String key = "test-key";
        byte[] content = "test content".getBytes();
        String contentType = "text/plain";

        // Act & Assert
        assertDoesNotThrow(() -> s3Service.uploadFileWithMetadata(bucketName, key, content, contentType));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void listObjects_shouldReturnObjectList() {
        // Arrange
        String bucketName = "test-bucket";
        S3Object mockObject = S3Object.builder().key("test-key").build();
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(mockObject)
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        // Act
        List<S3Object> result = s3Service.listObjects(bucketName);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().key()).isEqualTo("test-key");
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void listObjectsWithPrefix_shouldReturnFilteredObjects() {
        // Arrange
        String bucketName = "test-bucket";
        String prefix = "images/";
        S3Object mockObject = S3Object.builder().key("images/photo.jpg").build();
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(mockObject)
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        // Act
        List<S3Object> result = s3Service.listObjectsWithPrefix(bucketName, prefix);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().key()).isEqualTo("images/photo.jpg");
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }
}
