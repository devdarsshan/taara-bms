package com.taara.bms.dto.yarn;

import com.taara.bms.dto.common.StyleRefResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record YarnOrderResponse(
        UUID id,
        String autoId,
        LocalDate orderDate,
        StyleRefResponse style,
        BigDecimal quantityKgs,
        String supplierNotes,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
