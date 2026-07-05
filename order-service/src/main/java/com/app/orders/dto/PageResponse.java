package com.app.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PageResponse", description = "Generic paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "List of items in the current page")
    private List<T> content;

    @Schema(description = "Current page number (zero-based)", example = "0")
    private Integer page;

    @Schema(description = "Number of items per page", example = "10")
    private Integer size;

    @Schema(description = "Total number of items across all pages", example = "100")
    private Long totalElements;

    @Schema(description = "Total number of pages", example = "10")
    private Integer totalPages;

    @Schema(description = "Whether there is a next page", example = "true")
    private Boolean hasNext;
}

