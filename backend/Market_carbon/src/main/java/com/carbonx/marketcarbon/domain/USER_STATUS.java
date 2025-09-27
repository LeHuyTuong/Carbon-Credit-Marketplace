package com.carbonx.marketcarbon.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public enum USER_STATUS {
    ACTIVE,
    INACTIVE,
    BANNED,
    PENDING
}
