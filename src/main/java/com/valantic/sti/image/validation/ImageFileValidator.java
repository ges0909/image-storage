package com.valantic.sti.image.validation;

import com.valantic.sti.image.ImageProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class ImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    private static final Logger log = LoggerFactory.getLogger(ImageFileValidator.class);

    private final ImageProperties imageProperties;
    private long maxFileSize;
    private Set<String> allowedContentTypes;

    public ImageFileValidator() {
        this.imageProperties = null;
    }

    public ImageFileValidator(ImageProperties imageProperties) {
        this.imageProperties = imageProperties;
    }

    @Override
    public void initialize(ValidImageFile constraintAnnotation) {
        if (imageProperties != null) {
            this.maxFileSize = imageProperties.maxFileSize();
            this.allowedContentTypes = imageProperties.supportedTypes();
            log.debug("Initialized validator with maxFileSize: {} bytes, supportedTypes: {}", maxFileSize, allowedContentTypes);
        } else {
            this.maxFileSize = 10 * 1024 * 1024; // 10MB default
            this.allowedContentTypes = Set.of("image/jpeg", "image/png", "image/webp");
            log.warn("ImageProperties not available, using default values: maxFileSize={} bytes, supportedTypes={}", maxFileSize, allowedContentTypes);
        }
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            addViolation(context, "File cannot be empty");
            return false;
        }

        if (file.getSize() > maxFileSize) {
            addViolation(context, "File too large: " + file.getSize() + " bytes");
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !isValidImageType(contentType)) {
            addViolation(context, "Invalid image type: " + contentType);
            return false;
        }

        try {
            if (!FileSignatureValidator.isValidImageSignature(file.getInputStream(), contentType)) {
                addViolation(context, "File signature does not match content type");
                return false;
            }
        } catch (Exception e) {
            addViolation(context, "Unable to validate file signature");
            return false;
        }

        return true;
    }

    private boolean isValidImageType(String contentType) {
        return allowedContentTypes.contains(contentType);
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
