package com.valantic.sti.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagePropertiesTest {

    private static final String BUCKET = "test-bucket";
    private static final String THUMBNAILS = "test-thumbnails";
    private static final String KMS_KEY = "test-kms-key";
    private static final String CDN = "https://cdn.example.com";
    private static final long FILE_SIZE = 1048576L;
    private static final String REGION = "us-east-1";
    private static final int EXPIRATION = 15;
    private static final Set<String> CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final int MAX_RESULTS = 1000;
    private static final String KEY_PREFIX = "images";
    private static final int[] THUMBNAIL_SIZES = {150, 300, 600};

    @ParameterizedTest
    @ValueSource(ints = {-300, 0})
    void constructor_ShouldValidateThumbnailSizes_WhenInvalidProvided(int invalidSize) {
        assertThatThrownBy(() -> createProperties(new int[]{150, invalidSize, 600}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Thumbnail sizes must be positive");
    }

    @Test
    void constructor_ShouldAcceptEmptyThumbnailSizes() {
        ImageProperties properties = createProperties(new int[]{});

        assertThat(properties.thumbnailSizes()).isEmpty();
    }

    @Test
    void constructor_ShouldAcceptValidValues() {
        ImageProperties properties = createValidProperties();

        assertThat(properties.bucketName()).isEqualTo(BUCKET);
        assertThat(properties.thumbnailBucketName()).isEqualTo(THUMBNAILS);
        assertThat(properties.kmsKeyId()).isEqualTo(KMS_KEY);
        assertThat(properties.cloudfrontDomain()).isEqualTo(CDN);
        assertThat(properties.maxFileSize()).isEqualTo(FILE_SIZE);
        assertThat(properties.region()).isEqualTo(REGION);
        assertThat(properties.urlExpirationMinutes()).isEqualTo(EXPIRATION);
        assertThat(properties.maxResults()).isEqualTo(MAX_RESULTS);
        assertThat(properties.keyPrefix()).isEqualTo(KEY_PREFIX);
        assertThat(properties.thumbnailSizes()).containsExactly(150, 300, 600);
        assertThat(properties.supportedTypes()).containsExactlyInAnyOrder("image/jpeg", "image/png");
    }
    
    private ImageProperties createValidProperties() {
        return createProperties(THUMBNAIL_SIZES, CONTENT_TYPES);
    }

    private ImageProperties createProperties(int[] thumbnailSizes) {
        return createProperties(thumbnailSizes, Set.of("image/jpeg"));
    }

    private ImageProperties createProperties(int[] thumbnailSizes, Set<String> contentTypes) {
        return new ImageProperties(BUCKET, THUMBNAILS, KMS_KEY, CDN, FILE_SIZE,
            REGION, EXPIRATION, thumbnailSizes, contentTypes, MAX_RESULTS, KEY_PREFIX);
    }
}
