package com.valantic.sti.image;

import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.service.ImageService;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class UploadIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ImageService imageService;

    @Test
    void uploadImage_ShouldSucceed_WhenValidJpegFile() {
        MockMultipartFile file = createValidJpegFile();

        ImageResponse response = imageService.uploadImage(file, "Test Image", "Test Description", List.of("test"));

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Test Image");
        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.tags()).contains("test");
    }

    @Test
    void uploadImageAsync_ShouldSucceed_WhenValidFile() {
        MockMultipartFile file = createValidJpegFile();

        ImageResponse response = imageService.uploadImageAsync(file, "Async Image", "Async Description", List.of("async"));

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Async Image");
    }

    @Test
    void uploadImage_ShouldThrowException_WhenEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> imageService.uploadImage(emptyFile, "Test", "Desc", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadImage_ShouldThrowException_WhenInvalidContentType() {
        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());

        assertThatThrownBy(() -> imageService.uploadImage(textFile, "Test", "Desc", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadImage_ShouldHandleNullMetadata() {
        MockMultipartFile file = createValidJpegFile();

        ImageResponse response = imageService.uploadImage(file, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
    }
}
