package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.DiaRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExistingStockResponse(
        UUID id,
        String autoId,
        LocalDate entryDate,
        DiaRefResponse dia,
        StyleRefResponse style,
        BigDecimal quantityKgs,
        String notes,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
