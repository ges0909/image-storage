package com.valantic.sti.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Konfiguration für asynchrone Verarbeitung großer Images.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread Pool für Image-Upload und Thumbnail-Generierung.
     * Optimiert für I/O-intensive Operationen.
     */
    @Bean(name = "imageProcessingExecutor")
    public Executor imageProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ImageProcessing-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}