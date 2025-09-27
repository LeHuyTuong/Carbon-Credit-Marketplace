package com.carbonx.marketcarbon.domain;

import lombok.Getter;

@Getter
public enum StatusCode {

    SUCCESS("00000000", "Success"),
    CREATED("00000001", "Created"),
    BAD_REQUEST("04000000", "Bad Request"),
    UNAUTHORIZED("04000001", "Unauthorized"),
    FORBIDDEN("04000003", "Forbidden"),
    NOT_FOUND("04000004", "Not Found"),
    CONFLICT("04000009", "Conflict"),
    INTERNAL_SERVER_ERROR("05000000", "Internal Server Error");

    private final String code;
    private final String message;

    StatusCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
