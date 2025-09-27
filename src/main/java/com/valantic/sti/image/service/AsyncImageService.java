package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import io.micrometer.core.annotation.Timed;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchroner Service für Upload und Thumbnail-Generierung großer Bilder.
 * Optimiert für Performance bei Dateien bis 100MB.
 */
@Service
public class AsyncImageService {

    private static final Logger log = LoggerFactory.getLogger(AsyncImageService.class);

    private final S3TransferManager transferManager;
    private final S3AsyncClient s3AsyncClient;
    private final ImageMetadataRepository metadataRepository;
    private final ImageProperties imageProperties;

    public AsyncImageService(S3TransferManager transferManager, 
                           S3AsyncClient s3AsyncClient,
                           ImageMetadataRepository metadataRepository,
                           ImageProperties imageProperties) {
        this.transferManager = transferManager;
        this.s3AsyncClient = s3AsyncClient;
        this.metadataRepository = metadataRepository;
        this.imageProperties = imageProperties;
    }

    /**
     * Asynchroner Upload mit S3 Transfer Manager für große Dateien.
     */
    @Async
    @Timed("image.upload.async")
    public CompletableFuture<Void> uploadOriginalAsync(String imageId, byte[] imageData, 
                                                      String contentType, ImageMetadata metadata) {
        String key = "images/" + imageId + "/original";
        
        UploadRequest uploadRequest = UploadRequest.builder()
            .putObjectRequest(PutObjectRequest.builder()
                .bucket(imageProperties.bucketName())
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PRIVATE)
                .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                .ssekmsKeyId(imageProperties.kmsKeyId())
                .build())
            .requestBody(AsyncRequestBody.fromBytes(imageData))
            .build();

        Upload upload = transferManager.upload(uploadRequest);
        
        return upload.completionFuture()
            .thenRun(() -> {
                log.info("Original upload completed for image: {}", imageId);
                generateThumbnailsAsync(imageId, imageData, contentType, metadata);
            })
            .exceptionally(throwable -> {
                log.error("Original upload failed for image: {}", imageId, throwable);
                metadata.setStatus(ImageMetadata.UploadStatus.FAILED);
                metadataRepository.save(metadata);
                return null;
            });
    }

    /**
     * Asynchrone Thumbnail-Generierung mit Stream-Processing.
     */
    @Async
    @Timed("image.thumbnails.generation")
    public CompletableFuture<Void> generateThumbnailsAsync(String imageId, byte[] imageData, 
                                                          String contentType, ImageMetadata metadata) {
        return CompletableFuture.runAsync(() -> {
            try {
                int[] sizes = imageProperties.thumbnailSizes();
                
                for (int size : sizes) {
                    generateSingleThumbnail(imageId, imageData, contentType, size);
                }
                
                // Status auf COMPLETED setzen
                metadata.setStatus(ImageMetadata.UploadStatus.COMPLETED);
                metadataRepository.save(metadata);
                
                log.info("All thumbnails generated for image: {}", imageId);
                
            } catch (Exception e) {
                log.error("Thumbnail generation failed for image: {}", imageId, e);
                metadata.setStatus(ImageMetadata.UploadStatus.FAILED);
                metadataRepository.save(metadata);
                throw new ImageProcessingException("Thumbnail generation failed", e);
            }
        });
    }

    private void generateSingleThumbnail(String imageId, byte[] imageData, String contentType, int size) 
            throws IOException {
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Thumbnailator mit WebP für bessere Kompression
            String outputFormat = contentType.equals("image/png") ? "png" : "webp";
            
            Thumbnails.of(inputStream)
                .size(size, size)
                .keepAspectRatio(true)
                .outputQuality(0.85f)
                .outputFormat(outputFormat)
                .toOutputStream(outputStream);

            String thumbnailKey = "images/" + imageId + "/thumbnail_" + size;
            String thumbnailContentType = outputFormat.equals("webp") ? "image/webp" : contentType;

            // Asynchroner Upload des Thumbnails
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(imageProperties.thumbnailBucketName())
                .key(thumbnailKey)
                .contentType(thumbnailContentType)
                .acl(ObjectCannedACL.PRIVATE)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

            s3AsyncClient.putObject(putRequest, AsyncRequestBody.fromBytes(outputStream.toByteArray()))
                .join(); // Warten auf Completion
        }
    }
}