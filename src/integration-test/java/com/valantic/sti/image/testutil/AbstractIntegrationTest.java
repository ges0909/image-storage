package com.valantic.sti.image.testutil;

import com.valantic.sti.image.ImageProperties;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.util.Locale;

import static com.valantic.sti.image.testutil.TestConstants.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
        .withServices(S3)
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // AWS SDK standard properties
        registry.add("aws.accessKeyId", localstack::getAccessKey);
        registry.add("aws.secretAccessKey", localstack::getSecretKey);
        registry.add("aws.region", localstack::getRegion);

        // LocalStack S3 endpoint
        registry.add("aws.s3.endpoint", () -> localstack.getEndpointOverride(S3).toString());

        // Disable AWS config validation for tests
        registry.add("aws.config.validation.enabled", () -> "false");

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
            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
        );
        var region = Region.of(localstack.getRegion().toLowerCase(Locale.ROOT));

        try (S3Client s3Client = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(S3))
            .credentialsProvider(credentialsProvider)
            .region(region)
            .build()) {

            // Create buckets that the application expects
            s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET).build());
            s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_THUMBNAILS_BUCKET).build());
            s3Client.createBucket(CreateBucketRequest.builder().bucket("images-bucket-dev").build());
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
            localstack.getRegion().toLowerCase(Locale.ROOT),
            URL_EXPIRATION_MINUTES,
            THUMBNAIL_SIZES,
            java.util.Set.of("image/jpeg", "image/png", "image/webp"),
            MAX_RESULTS,
            TEST_KEY_PREFIX
        );
    }
}