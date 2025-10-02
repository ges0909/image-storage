package com.valantic.sti.image.repository;

import com.valantic.sti.image.entity.ImageMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, String> {

    Page<ImageMetadata> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT i FROM ImageMetadata i JOIN i.tags t WHERE t IN :tags")
    Page<ImageMetadata> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    Page<ImageMetadata> findByContentType(String contentType, Pageable pageable);

    Page<ImageMetadata> findByFileSizeBetween(Long minSize, Long maxSize, Pageable pageable);

    @Query("SELECT DISTINCT i FROM ImageMetadata i LEFT JOIN i.tags t WHERE " +
        "(:title IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
        "(:contentType IS NULL OR i.contentType = :contentType) AND " +
        "(:tags IS NULL OR t IN :tags) AND " +
        "i.status = 'COMPLETED'")
    Page<ImageMetadata> findBySearchCriteria(
        @Param("title") String title,
        @Param("contentType") String contentType,
        @Param("tags") List<String> tags,
        Pageable pageable
    );

    Page<ImageMetadata> findByStatus(ImageMetadata.UploadStatus status, Pageable pageable);

    @Query("SELECT COUNT(i), SUM(i.fileSize), AVG(i.fileSize) FROM ImageMetadata i WHERE i.status = 'COMPLETED'")
    Object[] getImageStatistics();
}
