package com.taara.bms.exception;

import java.util.Map;

public class BusinessValidationException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public BusinessValidationException(String code, String message) {
        this(code, message, Map.of());
    }

    public BusinessValidationException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
