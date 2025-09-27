package com.valantic.sti.image.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * JPA Entity für Image-Metadaten zur schnellen Suche und Filterung.
 * Ersetzt S3 ListObjects für bessere Performance bei großen Datenmengen.
 */
@Entity
@Table(name = "image_metadata", indexes = {
    @Index(name = "idx_title", columnList = "title"),
    @Index(name = "idx_content_type", columnList = "contentType"),
    @Index(name = "idx_upload_date", columnList = "uploadDate"),
    @Index(name = "idx_file_size", columnList = "fileSize")
})
@EntityListeners(AuditingEntityListener.class)
public class ImageMetadata {

    @Id
    private String imageId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "image_tags", joinColumns = @JoinColumn(name = "image_id"))
    @Column(name = "tag")
    private Set<String> tags;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String uploadedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status = UploadStatus.PROCESSING;

    // Constructors
    public ImageMetadata() {}

    public ImageMetadata(String imageId, String title, String description, Set<String> tags,
                        String contentType, Long fileSize, Integer width, Integer height,
                        String s3Key, String uploadedBy) {
        this.imageId = imageId;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.s3Key = s3Key;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters
    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public UploadStatus getStatus() { return status; }
    public void setStatus(UploadStatus status) { this.status = status; }

    public enum UploadStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}