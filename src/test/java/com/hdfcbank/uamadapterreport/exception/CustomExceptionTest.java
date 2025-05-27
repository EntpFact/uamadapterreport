package com.hdfcbank.uamadapterreport.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Something went wrong";
        CustomException exception = new CustomException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Error occurred";
        Throwable cause = new RuntimeException("Root cause");
        CustomException exception = new CustomException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
