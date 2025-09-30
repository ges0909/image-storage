package com.valantic.sti.image;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "management.endpoints.web.exposure.include=health,info",
    "management.endpoint.health.show-details=always",
    "management.health.s3.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:test_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "image.bucket-name=test-bucket",
    "image.thumbnail-bucket-name=test-thumbnails",
    "image.kms-key-id=test-key",
    "image.max-file-size=10485760",
    "image.region=us-east-1",
    "image.url-expiration-minutes=15",
    "image.thumbnail-sizes=150,300,600",
    "image.supported-types=image/jpeg,image/png,image/webp",
    "image.max-results=100",
    "image.key-prefix=images/"
})
public @interface IntegrationTest {
}
