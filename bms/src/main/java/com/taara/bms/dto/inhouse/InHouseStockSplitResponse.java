package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.DiaRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InHouseStockSplitResponse(
        UUID id,
        String autoId,
        UUID deliveryId,
        String deliveryAutoId,
        DiaRefResponse dia,
        StyleRefResponse style,
        BigDecimal quantityKgs,
        boolean canDelete,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
