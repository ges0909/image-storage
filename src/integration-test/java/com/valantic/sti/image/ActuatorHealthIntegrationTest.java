package com.valantic.sti.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("Actuator Health Endpoints")
class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Health Endpoints")
    class HealthEndpoints {

        @Test
        @DisplayName("Should return overall health status UP")
        void overallHealth_ShouldReturnUp() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode health = objectMapper.readTree(response.getBody());
            assertThat(health.get("status").asText()).isEqualTo("UP");
            assertThat(health.has("components")).isTrue();
        }

        @Test
        @DisplayName("Should return S3 health status")
        void s3Health_ShouldReturnStatus() throws Exception {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/s3", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode s3Health = objectMapper.readTree(response.getBody());
            assertThat(s3Health.get("status").asText()).isIn("UP", "DOWN");
            assertThat(s3Health.has("details")).isTrue();
            assertThat(s3Health.get("details").get("bucket").asText()).isEqualTo("test-bucket");
        }
    }

    @Nested
    @DisplayName("Info Endpoint")
    class InfoEndpoint {

        @Test
        @DisplayName("Should return application info")
        void applicationInfo_ShouldReturnInfo() {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }
    }
}
