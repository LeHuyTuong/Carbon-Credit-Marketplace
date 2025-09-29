package com.carbonx.marketcarbon.utils;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class        CommonResponse<T> {
    String requestTrace;   // requestTrace

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") // ISO 8601 format with timezone
    private OffsetDateTime responseDateTime;

    ResponseStatus responseStatus; // gồm responseCode, responseMessage

    T responseData; // dữ liệu trả về

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ResponseStatus {
        String responseCode;
        String responseMessage;
    }
}
