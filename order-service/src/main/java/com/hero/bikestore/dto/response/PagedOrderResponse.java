package com.hero.bikestore.dto.response;

import lombok.*;

import java.util.List;

/**
 * Paginated wrapper for admin order list responses.
 *
 * Mirrors the pagination metadata format used by bike-service
 * (page, size, totalElements, totalPages, last) so the frontend
 * can use the same pagination logic across all services.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedOrderResponse {

    private List<AdminOrderResponse> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
