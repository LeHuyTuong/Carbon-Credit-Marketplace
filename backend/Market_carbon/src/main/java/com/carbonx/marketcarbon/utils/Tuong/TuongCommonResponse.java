package com.carbonx.marketcarbon.utils.Tuong;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.ResponseStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TuongCommonResponse<T> {
    String requestTrace;
    String requestDateTime;
    TuongResponseStatus responseStatus;
    T response;


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
