package com.carbonx.marketcarbon.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CsvBatchException extends RuntimeException {
    private final List<?> errors;

    public CsvBatchException(List<?> errors) {
        super("CSV import failed");
        this.errors = errors;
    }
}
