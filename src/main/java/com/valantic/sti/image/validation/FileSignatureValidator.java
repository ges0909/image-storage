package com.valantic.sti.image.validation;

import java.io.IOException;
import java.io.InputStream;

public class FileSignatureValidator {

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_SIGNATURE = {0x47, 0x49, 0x46};
    private static final byte[] TIFF_SIGNATURE_LE = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_SIGNATURE_BE = {0x4D, 0x4D, 0x00, 0x2A};
    private static final byte[] BMP_SIGNATURE = {0x42, 0x4D};
    private static final byte[] HIF_SIGNATURE = {0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63}; // "ftypheic"
    private static final byte[] DICOM_SIGNATURE = {0x44, 0x49, 0x43, 0x4D}; // "DICM" at offset 128

    public static boolean isValidImageSignature(InputStream inputStream, String contentType) {
        try {
            byte[] header = new byte[12];
            int bytesRead = inputStream.read(header);
            if (bytesRead < 4) return false;

            return switch (contentType) {
                case "image/jpeg" -> matchesSignature(header, JPEG_SIGNATURE);
                case "image/png" -> matchesSignature(header, PNG_SIGNATURE);
                case "image/gif" -> matchesSignature(header, GIF_SIGNATURE);
                case "image/tiff" -> matchesSignature(header, TIFF_SIGNATURE_LE) || matchesSignature(header, TIFF_SIGNATURE_BE);
                case "image/bmp" -> matchesSignature(header, BMP_SIGNATURE);
                case "image/hif" -> matchesSignature(header, HIF_SIGNATURE);
                case "application/dicom" -> matchesSignature(header, DICOM_SIGNATURE);
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