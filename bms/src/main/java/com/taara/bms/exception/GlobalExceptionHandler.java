package com.taara.bms.exception;

import com.taara.bms.dto.common.ApiErrorResponse;
import com.taara.bms.dto.common.WarningResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler({BusinessValidationException.class, DeleteConflictException.class})
    public ResponseEntity<ApiErrorResponse> handleBusiness(RuntimeException ex) {
        if (ex instanceof BusinessValidationException businessEx) {
            return buildError(HttpStatus.BAD_REQUEST, businessEx.getCode(), businessEx.getMessage(), businessEx.getDetails());
        }
        DeleteConflictException deleteConflictException = (DeleteConflictException) ex;
        return buildError(HttpStatus.CONFLICT, deleteConflictException.getCode(), deleteConflictException.getMessage(), deleteConflictException.getDetails());
    }

    @ExceptionHandler(WarningRequiredException.class)
    public ResponseEntity<WarningResponse> handleWarning(WarningRequiredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new WarningResponse(ex.getCode().name(), ex.getMessage(), "Retry with overrideWarnings=true", ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleFallback(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String code,
            String message,
        Map<String, Object> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
