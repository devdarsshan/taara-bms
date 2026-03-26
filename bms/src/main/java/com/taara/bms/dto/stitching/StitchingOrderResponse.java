package com.taara.bms.dto.stitching;

import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.StitchingOrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StitchingOrderResponse(
        UUID id,
        String autoId,
        LocalDate orderDate,
        GarmentSize expectedSize,
        Integer expectedPieces,
        Integer deliveredPieces,
        Integer pendingPieces,
        Integer totalPiecesTaken,
        StitchingOrderStatus status,
        String notes,
        List<StitchingOrderRowResponse> rows,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
