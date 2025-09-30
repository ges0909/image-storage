package com.valantic.sti.image.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valantic.sti.image.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
@ContextConfiguration(classes = {BatchController.class, BatchControllerTest.TestConfig.class})
@Import(TestSecurityConfig.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Configuration
    static class TestConfig {
        @Bean
        public ImageService imageService() {
            return mock(ImageService.class);
        }
    }

    @Test
    void batchDeleteImages_ShouldReturn204_WhenValidRequest() throws Exception {
        List<String> imageIds = List.of(
            "12345678-1234-1234-1234-123456789012",
            "87654321-4321-4321-4321-210987654321"
        );

        mockMvc.perform(delete("/api/batch/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(imageIds)))
            .andExpect(status().isNoContent());

        verify(imageService).batchDeleteImages(imageIds);
    }

    @Test
    void batchDeleteImages_ShouldReturn400_WhenTooManyImages() throws Exception {
        List<String> imageIds = java.util.stream.IntStream.range(0, 101)
            .mapToObj(i -> "12345678-1234-1234-1234-12345678901" + String.format("%01d", i % 10))
            .toList();

        mockMvc.perform(delete("/api/batch/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(imageIds)))
            .andExpect(status().isBadRequest());
    }
}