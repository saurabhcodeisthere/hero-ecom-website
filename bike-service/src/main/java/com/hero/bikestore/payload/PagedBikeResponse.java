package com.hero.bikestore.payload;

import com.hero.bikestore.api.response.BikeResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
public class PagedBikeResponse {

    private List<BikeResponse> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
