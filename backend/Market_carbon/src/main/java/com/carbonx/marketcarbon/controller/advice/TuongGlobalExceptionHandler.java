package com.carbonx.marketcarbon.controller.advice;

import com.carbonx.marketcarbon.domain.StatusCode;
import com.carbonx.marketcarbon.exception.BadRequestException;
import com.carbonx.marketcarbon.exception.NotFoundException;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.exception.UnauthorizedException;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class TuongGlobalExceptionHandler {

    private TuongCommonResponse<Object> buildResp(HttpStatus status, String code, String message) {
        String trace = UUID.randomUUID().toString();
        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();
        TuongResponseStatus rs = new TuongResponseStatus(code, message);
        return new TuongCommonResponse<>(trace, now, rs, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildResp(HttpStatus.NOT_FOUND, StatusCode.NOT_FOUND.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildResp(HttpStatus.NOT_FOUND, "404", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildResp(HttpStatus.UNAUTHORIZED, "401", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResp(HttpStatus.BAD_REQUEST, "400", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Validation error"
                : ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResp(HttpStatus.BAD_REQUEST, "400", msg));
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<TuongCommonResponse<Object>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResp(HttpStatus.BAD_REQUEST, "400", msg.isEmpty() ? "Constraint violation" : msg));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Parameter '" + ex.getName() + "' must be of type " +
                (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResp(HttpStatus.BAD_REQUEST, "400", msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResp(HttpStatus.BAD_REQUEST, "400", "Malformed JSON request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TuongCommonResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResp(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Internal error"));
    }

}
