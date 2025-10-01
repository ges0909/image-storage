package com.valantic.sti.image.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
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
    @Index(name = "idx_content_type", columnList = "content_type"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_file_size", columnList = "file_size")
})
@EntityListeners(AuditingEntityListener.class)
public class ImageMetadata {

    @Id
    @Column(name = "image_id", length = 36)
    private String imageId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "image_tags", joinColumns = @JoinColumn(name = "image_id"))
    @Column(name = "tag", length = 100)
    private Set<String> tags;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "uploaded_by", nullable = false, length = 100)
    private String uploadedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "upload_status")
    private UploadStatus status = UploadStatus.PROCESSING;

    // Constructors
    public ImageMetadata() {
    }

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
    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    /**
     * Converts ImageMetadata to Map for S3 metadata storage.
     */
    public java.util.Map<String, String> toMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("title", title != null ? title : "");
        map.put("description", description != null ? description : "");
        map.put("tags", tags != null ? String.join(",", tags) : "");
        map.put("contentType", contentType != null ? contentType : "");
        map.put("fileSize", fileSize != null ? fileSize.toString() : "0");
        map.put("width", width != null ? width.toString() : "0");
        map.put("height", height != null ? height.toString() : "0");
        map.put("uploadedBy", uploadedBy != null ? uploadedBy : "");
        map.put("status", status != null ? status.name() : UploadStatus.PROCESSING.name());
        if (createdAt != null) map.put("createdAt", createdAt.toString());
        if (updatedAt != null) map.put("updatedAt", updatedAt.toString());
        return map;
    }

    public enum UploadStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
