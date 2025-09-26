package com.carbonx.marketcarbon.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String requestTrace;
    private final String requestDateTime;

    public ResourceNotFoundException(String message, String requestTrace, String requestDateTime) {
        super(message);
        this.requestTrace = requestTrace;
        this.requestDateTime = requestDateTime;
    }
    public ResourceNotFoundException(String message){
        this(message, null, null);
    }

}
