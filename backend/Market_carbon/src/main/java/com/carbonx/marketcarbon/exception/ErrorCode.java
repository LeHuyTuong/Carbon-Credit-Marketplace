package com.carbonx.marketcarbon.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    UNAUTHENTICATED(401, "You need to log in to perform this action.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(403, "You do not have permission", HttpStatus.FORBIDDEN),
    PASSWORD_EXISTED(409, "Password existed", HttpStatus.CONFLICT),
    ROLE_NOT_EXISTED(404, "Role not existed", HttpStatus.NOT_FOUND),
    INVALID_OTP(400, "OTP is invalid or expired", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_VERIFIED(403, "Account not verified by OTP", HttpStatus.FORBIDDEN),
    EMAIL_INVALID(400, "Email not existed", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(409, "Email already exists", HttpStatus.CONFLICT),
    CURRENT_PASSWORD_INVALID(400, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    CONFIRM_PASSWORD_INVALID(400, "Confirmed password is incorrect", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND(404, "Payment not found", HttpStatus.NOT_FOUND),
    EMAIL_CONTACT_INVALID(400, "Email Contact cannot be null", HttpStatus.BAD_REQUEST),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    EXPIRED_TOKEN(401, "EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED),
    USER_NOT_EXISTED(404, "User not existed", HttpStatus.NOT_FOUND),
    USER_EXISTED(400, "User existed", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(500, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    ROLE_NOT_FOUND(400,"Role not found", HttpStatus.NOT_FOUND),
    SELLER_NOT_FOUND(404, "Seller not found", HttpStatus.NOT_FOUND),
    VEHICLE_NOT_FOUND(404, "Vehicle not found", HttpStatus.NOT_FOUND),
    REPORT_INVALID_STATE(404,"Report closed",HttpStatus.NOT_FOUND),
    REPORT_NOT_FOUND(404, "Report not found", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(404, "Cannot read upload stream", HttpStatus.NOT_FOUND);

    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
