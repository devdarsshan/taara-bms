package com.taara.bms.dto.stitching;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.StitchingOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StitchingOrderResponse(
        UUID id,
        String autoId,
        LocalDate orderDate,
        StitchingSectionRefResponse stitchingSection,
        StyleRefResponse style,
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
