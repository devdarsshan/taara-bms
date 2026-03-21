package com.taara.bms.exception;

import com.taara.bms.enums.WarningCode;
import java.util.Map;

public class WarningRequiredException extends RuntimeException {

    private final WarningCode code;
    private final Map<String, Object> details;

    public WarningRequiredException(WarningCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public WarningCode getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
