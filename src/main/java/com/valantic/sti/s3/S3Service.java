package com.valantic.sti.s3;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class S3Service {

    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void uploadFile(String bucketName, String key, byte[] content) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build(),
            RequestBody.fromBytes(content)
        );
    }

    public void uploadFileWithMetadata(String bucketName, String key, byte[] content, String contentType) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .metadata(Map.of(
                    "uploaded-by", "s3-playground",
                    "file-size", String.valueOf(content.length)
                ))
                .build(),
            RequestBody.fromBytes(content)
        );
    }

    public byte[] downloadFile(String bucketName, String key) throws IOException {
        return s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()
        ).readAllBytes();
    }

    public List<S3Object> listObjects(String bucketName) {
        return s3Client.listObjectsV2(
            ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build()
        ).contents();
    }

    public List<S3Object> listObjectsWithPrefix(String bucketName, String prefix) {
        return s3Client.listObjectsV2(
            ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .maxKeys(100)
                .build()
        ).contents();
    }
}
