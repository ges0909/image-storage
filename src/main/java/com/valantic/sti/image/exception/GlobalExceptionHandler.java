package com.valantic.sti.image.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleImageNotFound(ImageNotFoundException ex) {
        log.warn("Image not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse("Image not found", ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(ImageProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleImageProcessing(ImageProcessingException ex) {
        log.error("Image processing error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("Image processing failed", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<Map<String, Object>> handleImageValidation(ImageValidationException ex) {
        log.warn("Image validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("Validation failed", ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("Invalid input", ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("Invalid request", "Validation failed", HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("Invalid argument", ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("Internal server error", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Map<String, Object> createErrorResponse(String error, String message, HttpStatus status) {
        return Map.of(
            "timestamp", LocalDateTime.now(),
            "status", status.value(),
            "error", error,
            "message", message
        );
    }
}