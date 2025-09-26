package com.valantic.sti.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
class S3ServiceIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3);

    private S3Client s3Client;
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();

        s3Service = new S3Service(s3Client);
    }

    @Test
    void listObjects_shouldReturnAllObjects() {
        // Arrange
        String bucketName = "list-objects-bucket";
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        s3Service.uploadFile(bucketName, "file1.txt", "content1".getBytes());
        s3Service.uploadFile(bucketName, "file2.txt", "content2".getBytes());

        // Act
        List<S3Object> result = s3Service.listObjects(bucketName);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(S3Object::key)
                .containsExactlyInAnyOrder("file1.txt", "file2.txt");
    }

    @Test
    void listObjectsWithPrefix_shouldReturnFilteredObjects() {
        // Arrange
        String bucketName = "list-objects-with-prefix-test-bucket";
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        s3Service.uploadFile(bucketName, "images/photo1.jpg", "image1".getBytes());
        s3Service.uploadFile(bucketName, "images/photo2.jpg", "image2".getBytes());
        s3Service.uploadFile(bucketName, "docs/readme.txt", "readme".getBytes());

        // Act
        List<S3Object> result = s3Service.listObjectsWithPrefix(bucketName, "images/");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(S3Object::key)
                .containsExactlyInAnyOrder("images/photo1.jpg", "images/photo2.jpg");
    }

    @Test
    void uploadAndDownloadFile_shouldWorkEndToEnd() throws IOException {
        // Arrange
        String bucketName = "upload-download-bucket";
        String key = "test-file.txt";
        byte[] content = "Hello Testcontainers!".getBytes();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        // Act
        s3Service.uploadFile(bucketName, key, content);
        byte[] result = s3Service.downloadFile(bucketName, key);

        // Assert
        assertThat(result).isEqualTo(content);
    }
}
