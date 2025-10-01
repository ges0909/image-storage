package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.model.ImageVersion;
import com.valantic.sti.image.repository.ImageMetadataRepository;
import io.micrometer.core.annotation.Timed;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final S3TransferManager transferManager;
    private final ImageMetadataRepository metadataRepository;
    private final ImageProperties imageProperties;

    public S3StorageService(S3Client s3Client,
                            S3AsyncClient s3AsyncClient,
                            S3TransferManager transferManager,
                            ImageMetadataRepository metadataRepository,
                            ImageProperties imageProperties) {
        this.s3Client = s3Client;
        this.s3AsyncClient = s3AsyncClient;
        this.transferManager = transferManager;
        this.metadataRepository = metadataRepository;
        this.imageProperties = imageProperties;
    }

    public void uploadImage(String key, byte[] data, String contentType, ImageMetadata metadata) {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(imageProperties.bucketName())
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PRIVATE)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(imageProperties.kmsKeyId())
                    .metadata(metadata.toMap())
                    .build(),
                RequestBody.fromBytes(data)
            );
            log.debug("Uploaded to S3: {}", key);
        } catch (S3Exception e) {
            log.error("S3 upload failed for key: {}", key, e);
            throw new ImageProcessingException("S3 upload failed", e);
        }
    }

    public void uploadThumbnail(String key, byte[] data, String contentType) {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(imageProperties.thumbnailBucketName())
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PRIVATE)
                    .build(),
                RequestBody.fromBytes(data)
            );
            log.debug("Uploaded thumbnail to S3: {}", key);
        } catch (S3Exception e) {
            log.error("S3 thumbnail upload failed for key: {}", key, e);
            throw new ImageProcessingException("S3 thumbnail upload failed", e);
        }
    }


    /**
     * Asynchronous upload with S3 Transfer Manager for large files.
     */
    @Async
    @Timed("image.upload.async")
    public void uploadImageAsync(String imageId, byte[] imageData, String contentType, ImageMetadata metadata) {
        String key = "images/" + imageId + "/original";

        UploadRequest uploadRequest = UploadRequest.builder()
            .putObjectRequest(PutObjectRequest.builder()
                .bucket(imageProperties.bucketName())
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PRIVATE)
                .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                .ssekmsKeyId(imageProperties.kmsKeyId())
                .metadata(metadata.toMap())
                .build())
            .build();

        Upload upload = transferManager.upload(uploadRequest);

        upload.completionFuture()
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
    public void generateThumbnailsAsync(String imageId,
                                        byte[] imageData,
                                        String contentType,
                                        ImageMetadata metadata) {
        CompletableFuture.runAsync(() -> {
            try {
                int[] sizes = imageProperties.thumbnailSizes();

                for (int size : sizes) {
                    generateSingleThumbnail(imageId, imageData, contentType, size);
                }

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

    public void deleteImage(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(imageProperties.bucketName())
                .key(key)
                .build());
            log.debug("Deleted from S3: {}", key);
        } catch (S3Exception e) {
            log.error("S3 delete failed for key: {}", key, e);
            throw new ImageProcessingException("S3 delete failed", e);
        }
    }

    public List<ImageVersion> getObjectVersions(String key) {
        try {
            ListObjectVersionsResponse response = s3Client.listObjectVersions(
                ListObjectVersionsRequest.builder()
                    .bucket(imageProperties.bucketName())
                    .prefix(key)
                    .build()
            );

            return response.versions().stream()
                .filter(version -> version.key().equals(key))
                .map(version -> new ImageVersion(
                    version.versionId(),
                    extractImageIdFromKey(key),
                    LocalDateTime.ofInstant(version.lastModified(), ZoneOffset.UTC),
                    version.size(),
                    version.isLatest(),
                    version.eTag()
                ))
                .toList();
        } catch (S3Exception e) {
            log.error("Failed to list versions for key: {}", key, e);
            throw new ImageProcessingException("Failed to list object versions", e);
        }
    }

    public void restoreVersion(String key, String versionId) {
        try {
            s3Client.copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(imageProperties.bucketName())
                    .sourceKey(key)
                    .sourceVersionId(versionId)
                    .destinationBucket(imageProperties.bucketName())
                    .destinationKey(key)
                    .acl(ObjectCannedACL.PRIVATE)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(imageProperties.kmsKeyId())
                    .build()
            );
            log.info("Restored version {} for key: {}", versionId, key);
        } catch (S3Exception e) {
            log.error("Failed to restore version {} for key: {}", versionId, key, e);
            throw new ImageProcessingException("Failed to restore object version", e);
        }
    }

    private void generateSingleThumbnail(String imageId, byte[] imageData, String contentType, int size)
        throws IOException {

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            String outputFormat = contentType.equals("image/png") ? "png" : "webp";

            Thumbnails.of(inputStream)
                .size(size, size)
                .keepAspectRatio(true)
                .outputQuality(0.85f)
                .outputFormat(outputFormat)
                .toOutputStream(outputStream);

            String thumbnailKey = "images/" + imageId + "/thumbnail_" + size;
            String thumbnailContentType = outputFormat.equals("webp") ? "image/webp" : contentType;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(imageProperties.thumbnailBucketName())
                .key(thumbnailKey)
                .contentType(thumbnailContentType)
                .acl(ObjectCannedACL.PRIVATE)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

            s3AsyncClient.putObject(putRequest, AsyncRequestBody.fromBytes(outputStream.toByteArray()))
                .join();
        }
    }

    private String extractImageIdFromKey(String key) {
        String filename = key.substring(key.lastIndexOf('/') + 1);
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
