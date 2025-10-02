package com.valantic.sti.image.config;

import com.valantic.sti.image.ImageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(ImageProperties.class)
public class S3Config {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
        @Value("${aws.accessKeyId:}") String accessKey,
        @Value("${aws.secretAccessKey:}") String secretKey) {

        // Use explicit credentials for local development (MinIO/LocalStack)
        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }

        // Use default credential provider chain for production (IAM roles, profiles, etc.)
        return DefaultCredentialsProvider.builder().build();
    }

    @Bean
    public S3Client s3Client(ImageProperties properties,
                             AwsCredentialsProvider credentialsProvider,
                             @Value("${aws.s3.endpoint:}") String endpoint) {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider);

        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                .forcePathStyle(true); // Path style für MinIO/LocalStack
        }
        // Für echtes AWS S3: Virtual Hosted Style (Standard)

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(ImageProperties properties,
                                   AwsCredentialsProvider credentialsProvider,
                                   @Value("${aws.s3.endpoint:}") String endpoint) {
        var builder = S3Presigner.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider);

        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient(ImageProperties properties,
                                       AwsCredentialsProvider credentialsProvider,
                                       @Value("${aws.s3.endpoint:}") String endpoint) {
        var builder = S3AsyncClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider);

        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                .forcePathStyle(true); // Path style für MinIO/LocalStack
        }
        // Für echtes AWS S3: Virtual Hosted Style (Standard)

        return builder.build();
    }

    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
            .s3Client(s3AsyncClient)
            .build();
    }
}
