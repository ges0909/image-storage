package com.valantic.sti.image.testutil;

import com.valantic.sti.image.ImageProperties;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Set;

import static com.valantic.sti.image.testutil.TestConstants.MAX_FILE_SIZE;
import static com.valantic.sti.image.testutil.TestConstants.MAX_RESULTS;
import static com.valantic.sti.image.testutil.TestConstants.TEST_BUCKET;
import static com.valantic.sti.image.testutil.TestConstants.TEST_CLOUDFRONT_DOMAIN;
import static com.valantic.sti.image.testutil.TestConstants.TEST_KEY_PREFIX;
import static com.valantic.sti.image.testutil.TestConstants.TEST_KMS_KEY;
import static com.valantic.sti.image.testutil.TestConstants.TEST_THUMBNAILS_BUCKET;
import static com.valantic.sti.image.testutil.TestConstants.THUMBNAIL_SIZES;
import static com.valantic.sti.image.testutil.TestConstants.URL_EXPIRATION_MINUTES;

public abstract class AbstractIntegrationTest {

    private static final GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
        .withExposedPorts(9000)
        .withEnv("MINIO_ROOT_USER", "testuser")
        .withEnv("MINIO_ROOT_PASSWORD", "testpassword123")
        .withCommand("server", "/data")
        .withReuse(true);

    static {
        minio.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // AWS SDK standard properties
        registry.add("aws.accessKeyId", () -> "testuser");
        registry.add("aws.secretAccessKey", () -> "testpassword123");
        registry.add("aws.region", () -> "us-east-1");

        // MinIO S3 endpoint
        registry.add("aws.s3.endpoint", () -> "http://localhost:" + minio.getMappedPort(9000));

        // Image service properties
        registry.add("image.bucket-name", () -> TEST_BUCKET);
        registry.add("image.thumbnail-bucket-name", () -> TEST_THUMBNAILS_BUCKET);
        registry.add("image.cloudfront-domain", () -> TEST_CLOUDFRONT_DOMAIN);
        registry.add("image.kms-key-id", () -> TEST_KMS_KEY);
        registry.add("image.key-prefix", () -> TEST_KEY_PREFIX);
    }

    @BeforeAll
    static void createTestBuckets() {
        var credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("testuser", "testpassword123")
        );
        var region = Region.US_EAST_1;

        try (S3Client s3Client = S3Client.builder()
            .endpointOverride(URI.create("http://localhost:" + minio.getMappedPort(9000)))
            .credentialsProvider(credentialsProvider)
            .region(region)
            .forcePathStyle(true)
            .build()) {

            // Create buckets that the application expects (ignore if already exist)
            createBucketIfNotExists(s3Client, TEST_BUCKET);
            createBucketIfNotExists(s3Client, TEST_THUMBNAILS_BUCKET);
            createBucketIfNotExists(s3Client, "dev-images-bucket");
        }
    }

    /**
     * Creates S3 bucket if it doesn't exist, ignoring BucketAlreadyOwnedByYouException.
     * <p>
     * This is crucial for container reuse (withReuse(true)):
     * - First test run: Creates buckets successfully
     * - Subsequent test runs: Buckets already exist in reused MinIO container
     * - Without this handling: BucketAlreadyOwnedByYouException would fail tests
     * - Container reuse improves test performance by avoiding container restart
     */
    private static void createBucketIfNotExists(S3Client s3Client, String bucketName) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            // Bucket already exists in reused container - this is expected and safe to ignore
        }
    }

    // Shared helper method for creating ImageProperties
    protected ImageProperties createTestImageProperties() {
        return new ImageProperties(
            TEST_BUCKET,
            TEST_THUMBNAILS_BUCKET,
            TEST_KMS_KEY,
            TEST_CLOUDFRONT_DOMAIN,
            MAX_FILE_SIZE,
            "us-east-1",
            URL_EXPIRATION_MINUTES,
            THUMBNAIL_SIZES,
            Set.of("image/jpeg", "image/png", "image/webp"),
            MAX_RESULTS,
            TEST_KEY_PREFIX
        );
    }

    protected MockMultipartFile createValidJpegFile() {
        // Create a minimal 1x1 pixel JPEG image programmatically
        try {
            java.awt.image.BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, 0xFF0000); // Red pixel

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "jpg", baos);

            return new MockMultipartFile("file", "test.jpg", "image/jpeg", baos.toByteArray());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to create test image", e);
        }
    }
}
