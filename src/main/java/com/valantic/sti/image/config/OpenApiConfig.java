package com.valantic.sti.image.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI imageStorageOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("🖼️ S3 Image Storage API")
                .description("Secure, scalable image storage service with AWS S3 backend")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("S3 Playground Team")
                    .email("team@valantic.com")
                    .url("https://github.com/valantic/image-storage"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server"),
                new Server()
                    .url("${app.api.production-url:https://api.example.com}")
                    .description("Production Server")
            ));
    }
}
