package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationException(NotificationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getErrorCode()); // e.g., "ERR_DB_002"
        body.put("message", ex.getMessage());

        // Determine Status Code based on Error Code
        HttpStatus status = HttpStatus.BAD_REQUEST; // Default 400

        if ("ERR_DB_002".equals(ex.getErrorCode())) {
            status = HttpStatus.NOT_FOUND; // 404 for "Not Found"
        }

        return new ResponseEntity<>(body, status);
    }
}