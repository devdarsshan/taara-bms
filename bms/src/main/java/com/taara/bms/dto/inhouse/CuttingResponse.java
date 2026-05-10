package com.taara.bms.dto.inhouse;

import com.taara.bms.enums.CuttingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CuttingResponse(
        UUID id,
        String autoId,
        LocalDate cuttingDate,
        BigDecimal totalQuantityUsedKgs,
        Integer totalOutputPieces,
        BigDecimal pcsPerKg,
        BigDecimal totalPrice,
        CuttingStatus status,
        String notes,
        List<CuttingRowResponse> rows,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
