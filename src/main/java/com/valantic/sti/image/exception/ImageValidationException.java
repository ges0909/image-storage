package com.valantic.sti.image.exception;

public class ImageValidationException extends RuntimeException {
    private final String field;
    private final Object rejectedValue;

    public ImageValidationException(String field, Object rejectedValue, String message) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }
}