package com.taara.bms.dto.printing;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrintingDeliveryResponse(
        UUID id,
        String autoId,
        LocalDate deliveryDate,
        String printingOrderAutoId,
        StitchingSectionRefResponse printingSection,
        StyleRefResponse style,
        GarmentSize size,
        Integer piecesDelivered,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
