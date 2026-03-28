package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.SplitStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record InHouseDeliveryResponse(
        UUID id,
        String autoId,
        LocalDate deliveryDate,
        UUID spinningDeliveryId,
        String spinningDeliveryAutoId,
        StyleRefResponse style,
        BigDecimal quantityKgs,
        BigDecimal allocatedQuantityKgs,
        SplitStatus splitStatus,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
