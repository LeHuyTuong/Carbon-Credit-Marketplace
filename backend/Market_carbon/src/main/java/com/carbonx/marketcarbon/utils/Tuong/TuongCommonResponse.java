package com.carbonx.marketcarbon.utils.Tuong;

import lombok.*;
import lombok.experimental.FieldDefaults;

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

}
