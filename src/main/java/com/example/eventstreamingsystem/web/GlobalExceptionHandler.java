package com.example.eventstreamingsystem.web;

import com.example.eventstreamingsystem.exception.PartitionNotFoundException;
import com.example.eventstreamingsystem.exception.TopicAlreadyExistsException;
import com.example.eventstreamingsystem.exception.TopicNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TopicNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTopicNotFound(TopicNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PartitionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePartitionNotFound(PartitionNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "PARTITION_NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TopicAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleTopicAlreadyExists(TopicAlreadyExistsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "TOPIC_ALREADY_EXISTS", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(), status.value(), code, message, path));
    }
}
