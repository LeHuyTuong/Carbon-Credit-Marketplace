package com.carbonx.marketcarbon.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregateChargingRequest {
    OffsetDateTime from;
    OffsetDateTime to;
    BigDecimal gridEmissionFactor; // kgCO2/kWh hoặc tCO2/MWh theo đơn vị bạn chuẩn hoá
    BigDecimal baselineIceCo2;     // giá trị baseline để so sánh
    List<Long> includeDataIds;     // tuỳ chọn: chỉ tính những record cụ thể
}
