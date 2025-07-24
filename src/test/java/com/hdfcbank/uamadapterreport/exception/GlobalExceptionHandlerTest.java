package com.hdfcbank.uamadapterreport.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void testHandleCustomException() {
        // Arrange
        CustomException exception = new CustomException("Invalid input");

        // Act
        ResponseEntity<String> response = exceptionHandler.handleCustomException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("An unexpected error occurred...", response.getBody());
    }
}
