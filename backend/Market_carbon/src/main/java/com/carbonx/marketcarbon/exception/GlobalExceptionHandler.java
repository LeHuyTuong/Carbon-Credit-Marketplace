package com.carbonx.marketcarbon.exception;

import com.carbonx.marketcarbon.utils.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private CommonResponse<Object> buildErrorResponse(String code, String message) {
        return CommonResponse.<Object>builder()
                .requestTrace(UUID.randomUUID().toString())
                .responseDateTime(OffsetDateTime.now())
                .responseStatus(CommonResponse.ResponseStatus.builder()
                        .responseCode(code)
                        .responseMessage(message)
                        .build())
                .responseData(null)
                .build();
    }

    // 400 - Bad Request (custom)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("400", ex.getMessage()));
    }

    // 401 - Unauthorized (custom)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<CommonResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse("401", ex.getMessage()));
    }

    // 403 - Forbidden (Spring Security)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse("403", "Forbidden"));
    }

    // 404 - Not Found (custom)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CommonResponse<Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse("404", ex.getMessage()));
    }

    // AppException -> dùng ErrorCode trong dự án
    @ExceptionHandler(AppException.class)
    public ResponseEntity<CommonResponse<Object>> handleAppException(AppException ex) {
        var ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getStatusCode())
                .body(buildErrorResponse(String.valueOf(ec.getCode()), ec.getMessage()));
    }

    // 400 - Validation (DTO @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Validation error"
                : ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("400", msg));
    }

    // 400 - Constraint (@Validated trên params)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Object>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("400", msg.isEmpty() ? "Constraint violation" : msg));
    }

    // 400 - Sai kiểu tham số
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Parameter '" + ex.getName() + "' must be of type " +
                (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("400", msg));
    }

    // 400 - JSON lỗi
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("400", "Malformed JSON request"));
    }

    // 500 - Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleOther(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("500", "Internal error"));
    }
}
