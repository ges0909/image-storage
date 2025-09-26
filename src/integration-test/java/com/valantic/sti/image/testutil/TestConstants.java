package com.valantic.sti.image.testutil;

/**
 * Shared test constants for consistent UUID formats and configuration values across all tests.
 */
public final class TestConstants {
    
    // Test UUIDs - consistent format across all tests
    public static final String TEST_IMAGE_ID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String NON_EXISTENT_ID = "123e4567-e89b-12d3-a456-426614174000";
    
    // Test data
    public static final String INVALID_TEXT = "invalid-image-data";
    public static final String SOME_TEXT = "some text";
    
    // Configuration constants
    public static final long MAX_FILE_SIZE = 10485760L; // 10MB
    public static final int URL_EXPIRATION_MINUTES = 15;
    public static final int[] THUMBNAIL_SIZES = {150, 300, 600};
    public static final int MAX_RESULTS = 1000;
    
    // Test bucket names
    public static final String TEST_BUCKET = "test-bucket";
    public static final String TEST_THUMBNAILS_BUCKET = "test-thumbnails";
    public static final String TEST_KMS_KEY = "test-kms-key";
    public static final String TEST_CLOUDFRONT_DOMAIN = "https://test-cdn.example.com";
    public static final String TEST_KEY_PREFIX = "images";
    
    private TestConstants() {
        // Utility class - prevent instantiation
    }
}