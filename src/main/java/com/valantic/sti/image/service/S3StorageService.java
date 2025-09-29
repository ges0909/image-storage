package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.model.ImageVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final ImageProperties imageProperties;

    public S3StorageService(S3Client s3Client, ImageProperties imageProperties) {
        this.s3Client = s3Client;
        this.imageProperties = imageProperties;
    }

    public void uploadImage(String key, byte[] data, String contentType, Map<String, String> metadata) {
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(imageProperties.bucketName())
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PRIVATE)
                    .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(imageProperties.kmsKeyId())
                    .metadata(metadata)
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

    private String extractImageIdFromKey(String key) {
        // Extract imageId from S3 key format (e.g., "images/uuid.jpg" -> "uuid")
        String filename = key.substring(key.lastIndexOf('/') + 1);
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}