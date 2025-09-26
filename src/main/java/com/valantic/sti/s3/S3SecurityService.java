package com.valantic.sti.s3;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3SecurityService {

    private final S3Client s3Client;

    public S3SecurityService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 1. Bucket-Level: ACL (Access Control List)
    public void createBucketWithACL(String bucketName) {
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .acl(BucketCannedACL.PRIVATE)
                .build());
    }

    // 2. Object-Level: ACL
    public void uploadFileWithACL(String bucketName, String key, byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .acl(ObjectCannedACL.PRIVATE)
                        .build(),
                RequestBody.fromBytes(content)
        );
    }

    // 3. Bucket-Level: Bucket Policy
    public void setBucketPolicy(String bucketName, String policyJson) {
        s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policyJson)
                .build());
    }

    // 4. Bucket-Level: Block Public Access
    public void blockPublicAccess(String bucketName) {
        s3Client.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
                .bucket(bucketName)
                .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
                        .blockPublicAcls(true)
                        .ignorePublicAcls(true)
                        .blockPublicPolicy(true)
                        .restrictPublicBuckets(true)
                        .build())
                .build());
    }

    // 5. Object-Level: Server-Side Encryption
    public void uploadFileWithEncryption(String bucketName, String key, byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                RequestBody.fromBytes(content)
        );
    }

    // 6. Object-Level: KMS Encryption
    public void uploadFileWithKMSEncryption(String bucketName, String key, byte[] content, String kmsKeyId) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                        .ssekmsKeyId(kmsKeyId)
                        .build(),
                RequestBody.fromBytes(content)
        );
    }

    // 7. Bucket-Level: Versioning (Security durch Backup)
    public void enableVersioning(String bucketName) {
        s3Client.putBucketVersioning(PutBucketVersioningRequest.builder()
                .bucket(bucketName)
                .versioningConfiguration(VersioningConfiguration.builder()
                        .status(BucketVersioningStatus.ENABLED)
                        .build())
                .build());
    }

    // 8. Bucket-Level: MFA Delete (Multi-Factor Authentication)
    // Note: MFA Delete requires special AWS credentials and cannot be enabled via standard SDK
    // Must be configured via AWS CLI with MFA token: aws s3api put-bucket-versioning --mfa

    // 9. Bucket-Level: Logging
    public void enableAccessLogging(String bucketName, String logBucket, String logPrefix) {
        s3Client.putBucketLogging(PutBucketLoggingRequest.builder()
                .bucket(bucketName)
                .bucketLoggingStatus(BucketLoggingStatus.builder()
                        .loggingEnabled(LoggingEnabled.builder()
                                .targetBucket(logBucket)
                                .targetPrefix(logPrefix)
                                .build())
                        .build())
                .build());
    }

    // 10. Object-Level: Pre-signed URLs (Temporärer Zugriff)
    public String generatePresignedUrl(String bucketName, String key, int durationMinutes) {
        try (S3Presigner presigner = S3Presigner.create()) {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(durationMinutes))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}