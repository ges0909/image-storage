package com.valantic.sti.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("Upload REST API Integration Tests")
class UploadRestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Synchronous Upload")
    class SyncUpload {

        @Test
        @DisplayName("Should upload image successfully via REST API")
        void uploadImage_ShouldSucceed_WhenValidRequest() throws Exception {
            // Given
            byte[] imageData = createJpegImageData();
            MultiValueMap<String, Object> body = createUploadRequest(imageData, "Test Image", "Description", "test,upload");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<String> response = restTemplate.postForEntity("/api/images", requestEntity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            
            JsonNode imageResponse = objectMapper.readTree(response.getBody());
            assertThat(imageResponse.get("id").asText()).isNotEmpty();
            assertThat(imageResponse.get("title").asText()).isEqualTo("Test Image");
            assertThat(imageResponse.get("description").asText()).isEqualTo("Description");
            assertThat(imageResponse.get("contentType").asText()).isEqualTo("image/jpeg");
            assertThat(imageResponse.get("tags").isArray()).isTrue();
        }

        @Test
        @DisplayName("Should return 400 for empty file")
        void uploadImage_ShouldReturn400_WhenEmptyFile() {
            // Given
            MultiValueMap<String, Object> body = createUploadRequest(new byte[0], "Test", "Desc", "tag");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange("/api/images", 
                HttpMethod.POST, requestEntity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 for invalid content type")
        void uploadImage_ShouldReturn400_WhenInvalidContentType() {
            // Given
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource("text content".getBytes()) {
                @Override
                public String getFilename() {
                    return "test.txt";
                }
            });
            body.add("title", "Test");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<String> response = restTemplate.postForEntity("/api/images", requestEntity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Asynchronous Upload")
    class AsyncUpload {

        @Test
        @DisplayName("Should upload image asynchronously via REST API")
        void uploadImageAsync_ShouldSucceed_WhenValidRequest() throws Exception {
            // Given
            byte[] imageData = createJpegImageData();
            MultiValueMap<String, Object> body = createUploadRequest(imageData, "Async Image", "Async Desc", "async");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<String> response = restTemplate.postForEntity("/api/images/async", requestEntity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            
            JsonNode imageResponse = objectMapper.readTree(response.getBody());
            assertThat(imageResponse.get("id").asText()).isNotEmpty();
            assertThat(imageResponse.get("title").asText()).isEqualTo("Async Image");
        }
    }

    @Nested
    @DisplayName("File Size Validation")
    class FileSizeValidation {

        @Test
        @DisplayName("Should accept file within size limit")
        void uploadImage_ShouldSucceed_WhenFileSizeValid() throws Exception {
            // Given - 1KB image (well within 10MB limit)
            byte[] imageData = createJpegImageData();
            MultiValueMap<String, Object> body = createUploadRequest(imageData, "Small Image", "Small", "size");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // When
            ResponseEntity<String> response = restTemplate.postForEntity("/api/images", requestEntity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    // Helper Methods

    private MultiValueMap<String, Object> createUploadRequest(byte[] imageData, String title, String description, String tags) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        body.add("file", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "test.jpg";
            }
        });
        
        if (title != null) body.add("title", title);
        if (description != null) body.add("description", description);
        if (tags != null) body.add("tags", tags);
        
        return body;
    }

    private byte[] createJpegImageData() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        // Create a simple pattern
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                image.setRGB(x, y, (x + y) % 2 == 0 ? 0xFF0000 : 0x00FF00); // Red/Green checkerboard
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}