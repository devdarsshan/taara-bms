package com.taara.bms.dto.stitching;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StitchingDeliveryResponse(
        UUID id,
        String autoId,
        LocalDate deliveryDate,
        UUID stitchingOrderId,
        String stitchingOrderAutoId,
        StitchingSectionRefResponse stitchingSection,
        StyleRefResponse style,
        Integer piecesDelivered,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
