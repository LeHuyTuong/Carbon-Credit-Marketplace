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
    FILE_UPLOAD_FAILED(404, "Cannot read upload stream", HttpStatus.NOT_FOUND),
    COMPANY_NOT_FOUND(404, "Company not found", HttpStatus.NOT_FOUND),
    PROJECT_NOT_FOUND(404, "Project not found", HttpStatus.NOT_FOUND),
    INVALID_STATE_TRANSITION(400, "This application has been forwarded to the CVA for review",HttpStatus.BAD_REQUEST),
    DUPLICATE_RESOURCE(400, "Duplicate tittle ",HttpStatus.BAD_REQUEST),
    INVALID_FINAL_APPROVAL_STATUS(400, "Status must be ADMIN_APPROVED or REJECTED", HttpStatus.BAD_REQUEST),
    CVA_NOT_FOUND(404,"CVA not found", HttpStatus.NOT_FOUND),
    ADMIN_NOT_FOUND(404,"Admin not found", HttpStatus.NOT_FOUND),
    VEHICLE_PLATE_EXISTS(404,"Admin not found", HttpStatus.NOT_FOUND),
    ONE_APPLICATION_PER_PROJECT(401, "Duplicate parentProjectId in CSV: each base project can only have one submission",HttpStatus.CONFLICT),
    //CSV IMPORT VALIDATION
    CSV_BASE_PROJECT_ID_INVALID(400, "baseProjectId must be a positive number (e.g. 1, 2, 3).", HttpStatus.BAD_REQUEST),
    CSV_TITLE_MISSING(400, "title must not be empty.", HttpStatus.BAD_REQUEST),
    CSV_DESCRIPTION_MISSING(400, "description is required for project context.", HttpStatus.BAD_REQUEST),
    CSV_LOGO_MISSING(400, "logo URL must not be empty.", HttpStatus.BAD_REQUEST),
    CSV_COMMITMENTS_MISSING(400, "commitments must not be empty.", HttpStatus.BAD_REQUEST),
    CSV_TECHNICAL_INDICATORS_MISSING(400, "technicalIndicators must not be empty.", HttpStatus.BAD_REQUEST),
    CSV_MEASUREMENT_METHOD_MISSING(400, "measurementMethod must not be empty.", HttpStatus.BAD_REQUEST),
    CSV_LEGAL_DOCS_URL_MISSING(400, "legalDocsUrl must not be empty.", HttpStatus.BAD_REQUEST),
    CSV_INVALID_URL_FORMAT(400, "Invalid URL format. Must start with http:// or https://", HttpStatus.BAD_REQUEST),
    CSV_INVALID_NUMBER_FORMAT(400, "Invalid numeric value in CSV field.", HttpStatus.BAD_REQUEST),
    CSV_MISSING_COLUMN(400, "Missing required column in CSV.", HttpStatus.BAD_REQUEST),
    CSV_MISSING_FIELD(400, "Missing required field in CSV row.", HttpStatus.BAD_REQUEST),
    CSV_INVALID_FILE_FORMAT(400, "Invalid CSV file format or unreadable content.", HttpStatus.BAD_REQUEST),
    CSV_UNEXPECTED_ERROR(500, "Unexpected error while importing CSV.", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCESS_DENIED(403,"Access denied: you do not have permission to perform this action.", HttpStatus.FORBIDDEN);

    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
