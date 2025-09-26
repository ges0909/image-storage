package com.valantic.sti.image;

import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = {
    "management.endpoints.web.exposure.include=health,info",
    "management.endpoint.health.show-details=always"
})
class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

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
        String response = restClient.get()
            .uri("/actuator/health")
            .retrieve()
            .body(String.class);

        assertThat(response).contains("\"status\":");
    }

    @Test
    void actuatorHealthS3_ShouldReturnS3HealthStatus() {
        try {
            String response = restClient.get()
                .uri("/actuator/health/s3")
                .retrieve()
                .body(String.class);

            assertThat(response).contains("\"status\":");
            assertThat(response).containsAnyOf("test-bucket", "images-bucket-dev", "bucket");
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

        // Info endpoint returns empty JSON by default
        assertThat(response).isNotNull();
    }
}
