package com.taara.bms.dto.common;

import java.util.Map;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, Object> details
) {
}
