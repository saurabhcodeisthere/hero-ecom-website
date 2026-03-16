package com.hero.bikestore;

import com.hero.bikestore.common.exception.base.BadRequestException;
import com.hero.bikestore.common.exception.base.ConflictException;
import com.hero.bikestore.common.exception.base.ResourceNotFoundException;
import com.hero.bikestore.common.exception.model.ErrorResponse;
import com.hero.bikestore.common.exception.model.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Plain unit test — no @SpringBootTest needed (this is a library, not an app!)
class CommonExceptionApplicationTests {

    @Test
    void resourceNotFoundException_hasCorrectErrorCode() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void badRequestException_hasCorrectErrorCode() {
        BadRequestException ex = new BadRequestException("Invalid input");
        assertEquals("BAD_REQUEST", ex.getErrorCode());
    }

    @Test
    void conflictException_hasCorrectErrorCode() {
        ConflictException ex = new ConflictException("Already exists");
        assertEquals("CONFLICT", ex.getErrorCode());
    }

    @Test
    void errorResponse_setsTimestampAndFields() {
        ErrorResponse response = new ErrorResponse("Something went wrong", "BAD_REQUEST");
        assertEquals("Something went wrong", response.getMessage());
        assertEquals("BAD_REQUEST", response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void apiResponse_setsAllFields() {
        ApiResponse<String> response = new ApiResponse<>(200, "Success", "some data");
        assertEquals(200, response.getStatus());
        assertEquals("Success", response.getMessage());
        assertEquals("some data", response.getData());
        assertNotNull(response.getTimestamp());
    }
}
