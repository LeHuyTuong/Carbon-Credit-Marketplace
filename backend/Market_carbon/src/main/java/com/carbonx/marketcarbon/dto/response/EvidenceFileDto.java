package com.carbonx.marketcarbon.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class EvidenceFileDto {
    Long id;
    String fileName;
    String contentType;
    Long fileSizeBytes;
    String storageUrl;
    OffsetDateTime uploadedAt;
    Boolean checkedByCva;
}