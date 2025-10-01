package com.valantic.sti.image;

import com.valantic.sti.image.config.SecurityConfig;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

    @Mock
    private SecurityConfig securityConfig;

    @Mock
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
