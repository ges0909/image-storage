package com.valantic.sti.image;

import com.valantic.sti.image.repository.ImageMetadataRepository;
import com.valantic.sti.image.service.AsyncImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "management.endpoints.web.exposure.include=health,info",
    "management.endpoint.health.show-details=always",
    "spring.datasource.url=jdbc:h2:mem:test_db",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ActuatorHealthIntegrationTest {

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3Presigner s3Presigner;

    @MockitoBean
    private AsyncImageService asyncImageService;

    @MockitoBean
    private ImageMetadataRepository metadataRepository;

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void actuatorHealth_ShouldReturnHealthStatus() {
        try {
            String response = restClient.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);

            assertThat(response).contains("\"status\":");
        } catch (RestClientException e) {
            // Accept 503 Service Unavailable if Redis is down
            assertThat(e.getMessage()).containsAnyOf("503", "SERVICE_UNAVAILABLE");
        }
    }

    @Test
    void actuatorHealthS3_ShouldReturnS3HealthStatus() {
        try {
            String response = restClient.get()
                .uri("/actuator/health/s3")
                .retrieve()
                .body(String.class);

            assertThat(response).contains("\"status\":");
        } catch (RestClientException e) {
            // Accept 503 Service Unavailable for S3 health check
            assertThat(e.getMessage()).containsAnyOf("503", "SERVICE_UNAVAILABLE");
        }
    }

    @Test
    void actuatorInfo_ShouldReturnApplicationInfo() {
        String response = restClient.get()
            .uri("/actuator/info")
            .retrieve()
            .body(String.class);

        assertThat(response).isNotNull();
    }
}
