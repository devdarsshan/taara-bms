package com.taara.bms.dto.packing;

import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PackingResponse(
        UUID id,
        String autoId,
        LocalDate packingDate,
        StyleRefResponse style,
        GarmentSize size,
        PackingStockType stockType,
        Integer correctlyPackedPieces,
        Integer defectivePieces,
        Integer totalConsumedPieces,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
