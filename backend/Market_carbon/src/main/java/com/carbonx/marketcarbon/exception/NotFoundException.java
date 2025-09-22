package com.carbonx.marketcarbon.exception;

public class NotFoundException extends RuntimeException { // catch in runtime
    public NotFoundException(String msg){ super(msg); }
}

