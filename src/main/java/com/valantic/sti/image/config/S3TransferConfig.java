package com.valantic.sti.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Konfiguration für S3 Transfer Manager - optimiert für große Dateien.
 */
@Configuration
public class S3TransferConfig {

    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
            .s3Client(s3AsyncClient)
            .build();
    }
}
