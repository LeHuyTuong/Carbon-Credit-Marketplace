package com.carbonx.marketcarbon.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // ==== AUTHENTICATION & AUTHORIZATION ====
    UNAUTHENTICATED(401, "You need to log in to perform this action.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(403, "You do not have permission.", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(403, "Access denied: you do not have permission to perform this action.", HttpStatus.FORBIDDEN),
    EXPIRED_TOKEN(401, "Token expired.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_VERIFIED(403, "Account not verified by OTP.", HttpStatus.FORBIDDEN),
    INVALID_KEY(1001, "Uncategorized error.", HttpStatus.BAD_REQUEST),

    // ==== USER & ROLE ====
    USER_NOT_EXISTED(404, "User not existed.", HttpStatus.NOT_FOUND),
    USER_EXISTED(409, "User already existed.", HttpStatus.CONFLICT),
    ROLE_NOT_EXISTED(404, "Role not existed.", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(404, "Role not found.", HttpStatus.NOT_FOUND),

    // ==== PASSWORD & EMAIL ====
    PASSWORD_EXISTED(409, "Password existed.", HttpStatus.CONFLICT),
    CURRENT_PASSWORD_INVALID(400, "Current password is incorrect.", HttpStatus.BAD_REQUEST),
    CONFIRM_PASSWORD_INVALID(400, "Confirmed password is incorrect.", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(404, "Email not existed.", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(409, "Email already exists.", HttpStatus.CONFLICT),
    EMAIL_SEND_FAILED(500, "Failed to send email.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_CONTACT_INVALID(400, "Email contact cannot be null.", HttpStatus.BAD_REQUEST),
    INVALID_OTP(400, "OTP is invalid or expired.", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(400, "OTP expired. Please register again.", HttpStatus.BAD_REQUEST),
    OTP_STILL_VALID(429,"OTP is still valid; please use the current OTP or try again later", HttpStatus.TOO_MANY_REQUESTS),
    // ==== ENTITY NOT FOUND ====
    SELLER_NOT_FOUND(404, "Seller not found.", HttpStatus.NOT_FOUND),
    VEHICLE_NOT_FOUND(404, "Vehicle not found.", HttpStatus.NOT_FOUND),
    COMPANY_NOT_FOUND(404, "Company not found.", HttpStatus.NOT_FOUND),
    PROJECT_NOT_FOUND(404, "Project not found.", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND(404, "Payment not found.", HttpStatus.NOT_FOUND),
    CVA_NOT_FOUND(404, "CVA not found.", HttpStatus.NOT_FOUND),
    ADMIN_NOT_FOUND(404, "Admin not found.", HttpStatus.NOT_FOUND),
    KYC_EXISTED(409, "KYC already exists.", HttpStatus.CONFLICT),
    TITTLE_DUPLICATED(409, "Title duplicated.", HttpStatus.CONFLICT),
    APPLICATION_EXISTED(409, "Application already exists.", HttpStatus.CONFLICT),
    APPLICATION_NOT_FOUND(404, "Application not found.", HttpStatus.NOT_FOUND),
    APPLICATION_DOCS_REQUIRED(404, "Required application doc", HttpStatus.NOT_FOUND),
    REPORT_DETAILS_NOT_FOUND(404, "Report details not found", HttpStatus.NOT_FOUND),

    // ==== VALIDATION & STATE ====
    INVALID_STATUS(400, "Invalid application status.", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION(400, "Invalid status transition.", HttpStatus.BAD_REQUEST),
    VEHICLE_PLATE_EXISTS(409, "Vehicle plate already exists.", HttpStatus.CONFLICT),
    REPORT_NOT_FOUND(404, "Report not found.", HttpStatus.NOT_FOUND),
    REPORT_INVALID_STATE(409, "Report is already closed.", HttpStatus.CONFLICT),
    INVALID_FINAL_APPROVAL_STATUS(400, "Status must be ADMIN_APPROVED or REJECTED.", HttpStatus.BAD_REQUEST),
    DUPLICATE_RESOURCE(409, "Duplicate resource.", HttpStatus.CONFLICT),
    ONE_APPLICATION_PER_PROJECT(409, "Each base project can only have one submission.", HttpStatus.CONFLICT),

    // ==== CSV IMPORT VALIDATION ====
    // 4001xx: lỗi cấu trúc CSV (thiếu header)
    CSV_MISSING_PROJECT_ID(400101, "CSV is missing 'project_id' column.", HttpStatus.BAD_REQUEST),
    CSV_MISSING_PERIOD(400102, "CSV is missing 'period' column.", HttpStatus.BAD_REQUEST),
    CSV_MISSING_TOTAL_ENERGY_OR_CHARGING(400103, "CSV needs 'total_energy' or fallback 'charging_energy'.", HttpStatus.BAD_REQUEST),
    CSV_MISSING_VEHICLE_COUNT_COLUMN(400104, "CSV needs vehicle count column: total_ev_owner/total_vehicles/total_vehicle/tong_xe.", HttpStatus.BAD_REQUEST),

    // 4002xx: lỗi dữ liệu CSV (không nhất quán/định dạng sai)
    CSV_INCONSISTENT_PROJECT_ID(400201, "All rows must have the same project_id.", HttpStatus.BAD_REQUEST),
    CSV_INCONSISTENT_PERIOD(400202, "All rows must have the same period.", HttpStatus.BAD_REQUEST),
    CSV_INCONSISTENT_TOTAL_ENERGY(400203, "Total energy must be identical across rows.", HttpStatus.BAD_REQUEST),
    CSV_VEHICLE_COUNT_MISSING(400204, "Vehicle count is missing.", HttpStatus.BAD_REQUEST),
    CSV_VEHICLE_COUNT_INVALID(400205, "Vehicle count must be a valid integer.", HttpStatus.BAD_REQUEST),
    CSV_TOTAL_ENERGY_NOT_FOUND(400206, "Total energy not found and no charging_energy to sum.", HttpStatus.BAD_REQUEST),
    CSV_PARSE_ERROR(400207, "Failed to parse CSV file: invalid format or missing required column.", HttpStatus.BAD_REQUEST),


    // 4091xx: xung đột
    REPORT_DUPLICATE_PERIOD(409101, "Report already exists for this seller/project/period.", HttpStatus.CONFLICT),

    CSV_MISSING_FIELD(400301, "CSV is missing a required field.", HttpStatus.BAD_REQUEST),
    CSV_INVALID_FILE_FORMAT(400302, "Invalid CSV file format.", HttpStatus.BAD_REQUEST), // có thể dùng 415 nếu muốn
    CSV_INVALID_NUMBER_FORMAT(400303, "Invalid number format in CSV.", HttpStatus.BAD_REQUEST),
    CSV_UNEXPECTED_ERROR(500201, "Unexpected error while processing CSV.", HttpStatus.INTERNAL_SERVER_ERROR),
    // 5xx: storage hoặc lỗi hệ thống
    STORAGE_UPLOAD_FAILED(500101, "Failed to store the uploaded file.", HttpStatus.INTERNAL_SERVER_ERROR),
    STORAGE_READ_FAILED(500102, "Failed to read the stored file.", HttpStatus.INTERNAL_SERVER_ERROR),
    // ==== GENERIC & FILE ====
    FILE_UPLOAD_FAILED(500, "Cannot read upload stream.", HttpStatus.INTERNAL_SERVER_ERROR),
    COMPANY_IS_EXIST(409, "Company already exists.", HttpStatus.CONFLICT),
    WALLET_NOT_ENOUGH_MONEY(400, "Wallet not enough money to withdraw.", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_MONEY_INVALID_AMOUNT(400, "Amount must be a positive integer. And the amount must be more 50.000 VND", HttpStatus.BAD_REQUEST),
    LISTING_IS_NOT_AVAILABLE(400, "Listing is not available.", HttpStatus.BAD_REQUEST),
    AMOUNT_IS_NOT_VALID(400, "Amount must be positive.", HttpStatus.BAD_REQUEST),
    AMOUNT_IS_NOT_ENOUGH(400, "Amount requested exceeds available balance.", HttpStatus.BAD_REQUEST),
    ORDER_IS_NOT_PENDING(400, "Order is not pending.", HttpStatus.BAD_REQUEST),
    ORDER_NOT_COMPLETED(400, "Order must be completed before payout.", HttpStatus.BAD_REQUEST),
    WALLET_IS_NOT_ENOUGH_MONEY(400, "Wallet does not have enough money.", HttpStatus.BAD_REQUEST),
    PAYOUT_ALREADY_PROCESSED(409, "Payout already processed for this order.", HttpStatus.CONFLICT),

    COMPANY_NOT_OWN(400, "Company does not own this carbon credit block.", HttpStatus.BAD_REQUEST),
    CVA_NOT_APPROVED(400, "CVA not approved.", HttpStatus.BAD_REQUEST),
    CARBON_CREDIT_NOT_PENDING(400, "Carbon credit not pending.", HttpStatus.BAD_REQUEST),
    CREDIT_BATCH_NOT_FOUND(404, "Creadit not found", HttpStatus.NOT_FOUND),

    REPORT_NOT_APPROVED(400, "Emission report must be approved before issuing credits.", HttpStatus.BAD_REQUEST),
    CREDIT_ALREADY_ISSUED(409, "Carbon credits already issued for this report.", HttpStatus.CONFLICT),
    CREDIT_QUANTITY_INVALID(400, "Computed credit quantity is invalid or zero.", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
