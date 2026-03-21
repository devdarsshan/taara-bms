package com.taara.bms.dto.masterdata;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiaResponse(
        UUID id,
        String autoId,
        String diaValue,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
