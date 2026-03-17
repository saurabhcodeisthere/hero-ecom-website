package com.hero.bikestore.controller;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.dto.BikeFilterRequest;
import com.hero.bikestore.model.Bike;
import com.hero.bikestore.payload.BikeDTO;
import com.hero.bikestore.payload.PagedBikeResponse;
import com.hero.bikestore.service.BikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/bikes")
@CrossOrigin(origins = "http://localhost:8080")
@RequiredArgsConstructor
@Tag(
        name = "Bike Catalog — Public",
        description = "Browse, search and filter the Hero bike catalog. All endpoints require a valid JWT."
)
public class BikeController {

    private final BikeService bikeService;

    @Operation(
            summary = "List all bikes (paginated)",
            description = "Returns a paginated list of all **active** bikes in the catalog. " +
                          "Use `sortBy` and `sortDir` to control ordering."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bikes fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PagedBikeResponse> getAllBikes(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of bikes per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g. id, price, modelName)", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(
                bikeService.getAllBikes(page, size, sortBy, sortDir)
        );
    }

    @Operation(
            summary = "Search bikes by keyword",
            description = "Full-text search across bike model name, description and type. Returns a paginated result."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    })
    @GetMapping("/search")
    public ResponseEntity<PagedBikeResponse> searchBikes(
            @Parameter(description = "Keyword matched against modelName, description, type", example = "Splendor", required = true)
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "modelName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(
                bikeService.searchBikes(query, page, size, sortBy, sortDir)
        );
    }

    @Operation(
            summary = "Get a single bike by ID",
            description = "Returns full details of one bike including all technical specifications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bike found and returned"),
            @ApiResponse(responseCode = "404", description = "No bike found with the given ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<BikeResponse> getBikeById(
            @Parameter(description = "Unique ID of the bike", example = "1", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                bikeService.getBikeById(id)
        );
    }

    @Operation(
            summary = "Filter bikes by technical specifications",
            description = "Apply one or more filters to narrow down the catalog. All parameters are optional — " +
                          "only the ones you provide are applied. " +
                          "Example: `type=COMMUTER&minPrice=80000&abs=true` returns ABS-equipped commuters above ₹80,000."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered bikes returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    })
    @GetMapping("/filter")
    public ResponseEntity<PagedBikeResponse> filterBikes(
            @Parameter(description = "Bike category (COMMUTER, SPORT, SCOOTER, CRUISER, ADVENTURE)", example = "COMMUTER")
            @RequestParam(required = false) String type,
            @Parameter(description = "Minimum price in INR", example = "70000")
            @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Maximum price in INR", example = "150000")
            @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Minimum engine displacement in cc", example = "100")
            @RequestParam(required = false) Integer minCc,
            @Parameter(description = "Maximum engine displacement in cc", example = "200")
            @RequestParam(required = false) Integer maxCc,
            @Parameter(description = "Engine type (e.g. Single Cylinder, Twin Cylinder)", example = "Single Cylinder")
            @RequestParam(required = false) String engineType,
            @Parameter(description = "Wheel type (e.g. Alloy, Spoke)", example = "Alloy")
            @RequestParam(required = false) String wheelType,
            @Parameter(description = "ABS equipped: true or false", example = "true")
            @RequestParam(required = false) Boolean abs,
            @Parameter(description = "Gearbox type (e.g. 5-Speed, CVT)", example = "5-Speed")
            @RequestParam(required = false) String gearbox,
            @Parameter(description = "Minimum fuel tank capacity in litres", example = "10")
            @RequestParam(required = false) Integer minFuelTank,
            @Parameter(description = "Minimum ground clearance in mm", example = "165")
            @RequestParam(required = false) Integer minGroundClearance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BikeFilterRequest filter = new BikeFilterRequest();
        filter.setType(type);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setMinCc(minCc);
        filter.setMaxCc(maxCc);
        filter.setEngineType(engineType);
        filter.setWheelType(wheelType);
        filter.setAbs(abs);
        filter.setGearbox(gearbox);
        filter.setMinFuelTank(minFuelTank);
        filter.setMinGroundClearance(minGroundClearance);

        return ResponseEntity.ok(
                bikeService.filterBikes(filter, page, size)
        );
    }
}
