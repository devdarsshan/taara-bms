package com.taara.bms.dto.common;

import java.util.Map;

public record WarningResponse(
        String code,
        String message,
        String action,
        Map<String, Object> details
) {
}
