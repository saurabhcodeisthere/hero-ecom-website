package com.hero.bikestore.service.impl;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.dto.BikeFilterRequest;
import com.hero.bikestore.exception.*;
import com.hero.bikestore.mapper.BikeMapper;
import com.hero.bikestore.model.Bike;

import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.payload.PagedBikeResponse;
import com.hero.bikestore.repository.BikeRepository;
import com.hero.bikestore.repository.specifications.BikeSpecification;
import com.hero.bikestore.service.BikeService;
import com.hero.bikestore.service.FileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BikeServiceImpl implements BikeService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private BikeRepository bikeRepository;

    //@Value("${project.image}")
    private String uploadDir="xyz";// injected via Lombok constructor

    @Autowired
    private BikeMapper bikeMapper;

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "modelName", "price", "type");

    @Override
    public PagedBikeResponse getAllBikes(int page, int size, String sortBy, String sortDir) {

        validatePagination(page, size, sortBy, sortDir);


        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Bike> bikePage = bikeRepository.findByActiveTrue(pageable);

        return new PagedBikeResponse(
                bikePage.getContent()
                        .stream()
                        .map(bikeMapper::toResponse)
                        .toList(),
                bikePage.getNumber(),
                bikePage.getSize(),
                bikePage.getTotalElements(),
                bikePage.getTotalPages(),
                bikePage.isLast()
        );
    }


    @Override
    public PagedBikeResponse searchBikes(String query, int page, int size, String sortBy, String sortDir) {

        validateSearch(query, page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Bike> bikePage =
                bikeRepository.findByModelNameContainingIgnoreCaseAndActiveTrue(
                        query.trim(),
                        pageable
                );

        return new PagedBikeResponse(
                bikePage.getContent()
                        .stream()
                        .map(bikeMapper::toResponse)
                        .toList(),
                bikePage.getNumber(),
                bikePage.getSize(),
                bikePage.getTotalElements(),
                bikePage.getTotalPages(),
                bikePage.isLast()
        );
    }

    @Override
    public BikeResponse getBikeById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Bike id must be a positive number");
        }

        Bike bike = bikeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bike not found with id: " + id
                        )
                );
        return bikeMapper.toResponse(bike);
    }

    @Override
    public BikeResponse updateBike(Long id, BikeRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Bike id must be a positive number");
        }

        Bike existingBike = bikeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bike not found with id: " + id
                        )
                );

        // Update entity fields
        bikeMapper.updateEntity(existingBike, request);

        Bike updatedBike = bikeRepository.save(existingBike);

        return bikeMapper.toResponse(updatedBike);
    }


//    @Override
//    public Bike getBikeById(Long id) {
//        return bikeRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Bike not found with id: " + id));
//    }


//    @Override
//    public BikeDTO updateBikeImage(Long bikeId, MultipartFile file) {
//
//        if (bikeId == null) {
//            throw new BadRequestException("Bike id is required.");
//        }
//        if (file == null || file.isEmpty()) {
//            throw new BadRequestException("Image file must be provided.");
//        }
//
//        // load bike
//        Bike bike = bikeRepository.findById(bikeId)
//                .orElseThrow(() -> new ResourceNotFoundException("Bike not found with id: " + bikeId));
//
//        // store file and get stored filename
//        String storedFileName = fileService.storeFile(uploadDir, file);
//
//        // Optionally: build a public URL or save only file name. Here we store the relative path:
//        // e.g. "uploads/bikes/<storedfile>"
//        String savedImagePath = uploadDir.endsWith("/") ? uploadDir + storedFileName : uploadDir + "/" + storedFileName;
//
//        // Save only filename or path depending on your usage. If you only want filename:
//        // bike.setImageUrl(storedFileName);
//        // I recommend storing relative path so you can serve static files easily:
//        bike.setImageUrl(savedImagePath);
//
//        // persist
//        Bike saved = bikeRepository.save(bike);
//
//        // map to DTO
//        return modelMapper.map(saved, BikeDTO.class);
//    }



//    @Override
//    public List<Bike> filterBikes(Integer minCc, Integer maxCc, Double minPrice, Double maxPrice) {
//        // We'll implement this properly after testing basic flow
//        return bikeRepository.findAll();
//    }


    @Override
    public BikeResponse deactivateBike(Long id) {

        validateId(id);

        Bike bike = bikeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Bike not found with id: " + id)
                );

        if (!bike.isActive()) {
            return bikeMapper.toResponse(bike); // idempotent
        }

        bike.setActive(false);
        return bikeMapper.toResponse(bike);
    }



    private void validateFilter(BikeFilterRequest filter, int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }

        if (size <= 0 || size > 50) {
            throw new IllegalArgumentException("Size must be between 1 and 50");
        }

        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice() > filter.getMaxPrice()) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        if (filter.getMinCc() != null && filter.getMaxCc() != null
                && filter.getMinCc() > filter.getMaxCc()) {
            throw new IllegalArgumentException("minCc cannot be greater than maxCc");
        }
    }


    @Override
    public BikeResponse activateBike(Long id) {

        validateId(id);

        Bike bike = bikeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Bike not found with id: " + id)
                );

        if (bike.isActive()) {
            return bikeMapper.toResponse(bike); // idempotent
        }

        bike.setActive(true);
        return bikeMapper.toResponse(bike);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedBikeResponse filterBikes(BikeFilterRequest filter, int page, int size) {

        validateFilter(filter, page, size);

        Pageable pageable = PageRequest.of(page, size);

        Specification<Bike> spec = BikeSpecification.build(filter);

        Page<Bike> result = bikeRepository.findAll(spec, pageable);

        return new PagedBikeResponse(
                result.getContent().stream()
                        .map(bikeMapper::toResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Bike id must be a positive number");
        }
    }


    @Override
    public BikeResponse addBike(BikeRequest bikeRequest) {

        // 1️⃣ Business validation
        String slug = generateSlug(bikeRequest.getModelName());

        if (bikeRepository.existsBySlug(slug)) {
            throw new BikeAlreadyExistsException(
                    "Bike with model name already exists: " + bikeRequest.getModelName()
            );
        }

        // 2️⃣ Request → Entity
        Bike bikeEntity = bikeMapper.toEntity(bikeRequest);
        bikeEntity.setSlug(slug);
        bikeEntity.setActive(true);

        // 3️⃣ Save to DB
        Bike savedBike = bikeRepository.save(bikeEntity);

        // 4️⃣ Entity → Response
        return bikeMapper.toResponse(savedBike);
    }

    private String generateSlug(String modelName) {
        return modelName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }


    private void validatePagination(int page, int size, String sortBy, String sortDir) {

        if (page < 0) {
            throw new InvalidPaginationException("Page number must be >= 0");
        }

        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException("Page size must be between 1 and 50");
        }

        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new InvalidPaginationException("sortDir must be 'asc' or 'desc'");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidPaginationException("Invalid sortBy field: " + sortBy);
        }
    }








    private void validateSearch(String query, int page, int size, String sortBy, String sortDir) {

        if (query == null || query.trim().length() < 2) {
            throw new InvalidSearchException(
                    "Search query must contain at least 2 characters"
            );
        }

        if (page < 0) {
            throw new InvalidSearchException("Page number must be >= 0");
        }

        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new InvalidSearchException(
                    "Page size must be between 1 and 50"
            );
        }

        if (!sortDir.equalsIgnoreCase("asc")
                && !sortDir.equalsIgnoreCase("desc")) {
            throw new InvalidSearchException(
                    "sortDir must be 'asc' or 'desc'"
            );
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidSearchException(
                    "Invalid sortBy field: " + sortBy
            );
        }
    }

}
