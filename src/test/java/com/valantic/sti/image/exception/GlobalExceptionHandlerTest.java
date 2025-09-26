package com.valantic.sti.image.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleImageNotFound_ShouldReturn404() {
        ImageNotFoundException ex = new ImageNotFoundException("Image not found");

        ResponseEntity<Map<String, Object>> response = handler.handleImageNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Image not found");
    }

    @Test
    void handleImageProcessing_ShouldReturn500() {
        ImageProcessingException ex = new ImageProcessingException("Processing failed");

        ResponseEntity<Map<String, Object>> response = handler.handleImageProcessing(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Image processing failed");
    }

    @Test
    void handleImageValidation_ShouldReturn400() {
        ImageValidationException ex = new ImageValidationException("field", "value", "Invalid value");

        ResponseEntity<Map<String, Object>> response = handler.handleImageValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Validation failed");
    }

    @Test
    void handleIllegalArgument_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Invalid argument");
    }

    @Test
    void handleGeneral_ShouldReturn500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Internal server error");
    }
}