package com.taara.bms.dto.printing;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.StitchingOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrintingOrderResponse(
        UUID id,
        String autoId,
        LocalDate orderDate,
        StitchingSectionRefResponse printingSection,
        StyleRefResponse style,
        GarmentSize size,
        Integer piecesOrdered,
        Integer deliveredPieces,
        Integer pendingPieces,
        StitchingOrderStatus status,
        String notes,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
