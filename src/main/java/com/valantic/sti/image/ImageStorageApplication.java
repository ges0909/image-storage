package com.valantic.sti.image;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ImageStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageStorageApplication.class, args);
    }
}
