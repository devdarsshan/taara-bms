package com.taara.bms.dto.masterdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StyleResponse(
        UUID id,
        String autoId,
        String styleName,
        List<String> colors,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
