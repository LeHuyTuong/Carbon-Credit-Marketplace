package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.CreditStatus;
import lombok.Builder;

@Builder
public record CreditQuery(Long projectId,
                          Integer vintageYear,
                          CreditStatus status) { }
