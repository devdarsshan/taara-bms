package com.taara.bms.dto.spinning;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.PaymentStatus;
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
        StitchingSectionRefResponse stitchingSection,
        BigDecimal pricePerKg,
        BigDecimal totalPrice,
        BigDecimal paidAmount,
        BigDecimal balanceAmount,
        PaymentStatus paymentStatus,
        String notes,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
