package com.hero.bikestore.service;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.dto.BikeFilterRequest;
import com.hero.bikestore.model.Bike;
import com.hero.bikestore.payload.BikeDTO;
import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.payload.PagedBikeResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface BikeService {

    PagedBikeResponse getAllBikes(int page, int size, String sortBy, String sortDir);
    PagedBikeResponse searchBikes(String query, int page, int size, String sortBy, String sortDir);

    //BikeDTO updateBikeImage(Long bikeId, MultipartFile file);

    //List<Bike> filterBikes(Integer minCc, Integer maxCc, Double minPrice, Double maxPrice);

    BikeResponse getBikeById(Long id);

    BikeResponse addBike(BikeRequest bikeRequest);
    BikeResponse updateBike(Long id, BikeRequest request);

    BikeResponse deactivateBike(Long id);

    BikeResponse activateBike(Long id);

    PagedBikeResponse filterBikes(BikeFilterRequest filter, int page, int size);

}
