package com.hero.bikestore.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    /** ✅ Indicates whether the operation succeeded or failed */
    private boolean success;

    /** ✅ Human-readable message to describe the result */
    private String message;

    /** ✅ Generic payload (can be any type: DTO, List, Page, etc.) */
    private T data;
}
