package com.carbonx.marketcarbon.utils.common;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    String requestTrace;
    String requestDateTime;
    ResponseStatus responseStatus;
    T response;

}
