package com.hero.bikestore.exception;

import com.hero.bikestore.payload.ApiResponse;
import com.hero.bikestore.payload.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message("Internal server error: " + ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(BikeAlreadyExistsException.class)
    public ResponseEntity<String> handleBikeAlreadyExists(BikeAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPagination(
            InvalidPaginationException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidSearchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSearch(
            InvalidSearchException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage()
                ));
    }
}
