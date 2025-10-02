package com.valantic.sti.image;

import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.ImageSize;
import com.valantic.sti.image.service.ImageService;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class ImageUrlIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImageService imageService;

    @Test
    void generateSignedUrl_ShouldThrowException_WhenImageNotFound() {
        assertThatThrownBy(() -> imageService.generateSignedUrl("non-existent-id", ImageSize.ORIGINAL, Duration.ofMinutes(10)))
            .hasMessageContaining("not found");
    }

    @Test
    void getThumbnailUrl_ShouldThrowException_WhenOriginalSize() {
        assertThatThrownBy(() -> imageService.getThumbnailUrl("test-id", ImageSize.ORIGINAL))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Use generateSignedUrl for original images");
    }

    @Test
    void uploadAndGenerateUrl_ShouldWork_EndToEnd() {
        // Upload image first
        MockMultipartFile file = createValidJpegFile();
        ImageResponse response = imageService.uploadImage(file, "Test", "Desc", null);

        // Generate signed URL
        String signedUrl = imageService.generateSignedUrl(response.id(), ImageSize.ORIGINAL, Duration.ofMinutes(5));

        assertThat(signedUrl).isNotNull();
        assertThat(signedUrl).contains(response.id());
    }

    @Test
    void generateThumbnailUrl_ShouldWork_AfterUpload() {
        // Upload image first
        MockMultipartFile file = createValidJpegFile();
        ImageResponse response = imageService.uploadImage(file, "Test", "Desc", null);

        // Generate thumbnail URL
        String thumbnailUrl = imageService.getThumbnailUrl(response.id(), ImageSize.THUMBNAIL_300);

        assertThat(thumbnailUrl).isNotNull();
        assertThat(thumbnailUrl).contains(response.id());
        assertThat(thumbnailUrl).contains("300");
    }
}
