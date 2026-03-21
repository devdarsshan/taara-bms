package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.DiaRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.CuttingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CuttingResponse(
        UUID id,
        String autoId,
        LocalDate cuttingDate,
        DiaRefResponse dia,
        StyleRefResponse style,
        BigDecimal quantityUsedKgs,
        Integer outputPieces,
        CuttingStatus status,
        String notes,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
