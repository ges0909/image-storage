package com.valantic.sti.image.api;

import com.valantic.sti.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch Operations", description = "🚀 Batch operations for improved performance")
public class BatchController {

    private final ImageService imageService;

    public BatchController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Deletes multiple images in a single batch operation for improved performance.
     * 
     * @param imageIds list of image UUIDs to delete (1-100 images)
     * @return empty response with 204 No Content status
     */
    @Operation(summary = "Batch Delete Images", description = "Delete multiple images in a single operation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Images deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or too many images")
    })
    @DeleteMapping("/images")
    public ResponseEntity<Void> batchDeleteImages(
        @Parameter(description = "List of image IDs to delete (max 100)")
        @RequestBody @Valid @Size(min = 1, max = 100) List<@Pattern(regexp = "[a-fA-F0-9-]{36}", message = "Invalid UUID format") String> imageIds) {
        
        imageService.batchDeleteImages(imageIds);
        return ResponseEntity.noContent().build();
    }
}