package com.valantic.sti.image.validation;

import java.io.IOException;
import java.io.InputStream;

public class FileSignatureValidator {

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_SIGNATURE = {0x52, 0x49, 0x46, 0x46};

    public static boolean isValidImageSignature(InputStream inputStream, String contentType) {
        try {
            byte[] header = new byte[12];
            int bytesRead = inputStream.read(header);
            if (bytesRead < 4) return false;

            return switch (contentType) {
                case "image/jpeg" -> matchesSignature(header, JPEG_SIGNATURE);
                case "image/png" -> matchesSignature(header, PNG_SIGNATURE);
                case "image/webp" -> matchesSignature(header, WEBP_SIGNATURE);
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean matchesSignature(byte[] header, byte[] signature) {
        if (header.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) return false;
        }
        return true;
    }
}