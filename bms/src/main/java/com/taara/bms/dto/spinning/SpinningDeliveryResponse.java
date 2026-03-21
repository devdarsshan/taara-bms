package com.taara.bms.dto.spinning;

import com.taara.bms.dto.common.StyleRefResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SpinningDeliveryResponse(
        UUID id,
        String autoId,
        LocalDate deliveryDate,
        StyleRefResponse style,
        BigDecimal actualQuantityKgs,
        BigDecimal bufferQuantityKgs,
        BigDecimal finalQuantityKgs,
        String notes,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
