package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.model.ImageDimensions;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);

    private final ImageProperties imageProperties;
    private final S3StorageService s3StorageService;

    public ImageProcessingService(ImageProperties imageProperties, S3StorageService s3StorageService) {
        this.imageProperties = imageProperties;
        this.s3StorageService = s3StorageService;
    }

    public ImageDimensions getImageDimensions(byte[] imageData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new ImageProcessingException("Invalid image format");
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            log.error("Failed to read image dimensions", e);
            throw new ImageProcessingException("Failed to read image dimensions", e);
        }
    }

    public void generateThumbnails(String imageId, byte[] originalData, String contentType) {
        for (int size : imageProperties.thumbnailSizes()) {
            generateThumbnail(imageId, originalData, size);
        }
    }

    private void generateThumbnail(String imageId, byte[] originalData, int size) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(originalData);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Thumbnails.of(bis)
                .size(size, size)
                .keepAspectRatio(true)
                .outputFormat("webp")
                .outputQuality(0.8)
                .toOutputStream(bos);

            String thumbnailKey = "thumbnails/" + imageId + "/" + size + ".webp";
            s3StorageService.uploadThumbnail(thumbnailKey, bos.toByteArray(), "image/webp");

            log.debug("Generated thumbnail {}x{} for image: {}", size, size, imageId);

        } catch (IOException e) {
            log.error("Failed to generate thumbnail {}x{} for image: {}", size, size, imageId, e);
            throw new ImageProcessingException("Thumbnail generation failed", e);
        }
    }

    public boolean isValidImageFormat(String contentType) {
        return imageProperties.allowedContentTypes().contains(contentType);
    }
}
