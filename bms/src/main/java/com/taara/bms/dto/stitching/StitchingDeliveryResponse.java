package com.taara.bms.dto.stitching;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StitchingDeliveryResponse(
        UUID id,
        String autoId,
        LocalDate deliveryDate,
        StitchingSectionRefResponse stitchingSection,
        StyleRefResponse style,
        GarmentSize size,
        Integer piecesDelivered,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
