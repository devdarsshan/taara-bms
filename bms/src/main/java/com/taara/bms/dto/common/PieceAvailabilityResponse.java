package com.taara.bms.dto.common;

import com.taara.bms.enums.GarmentSize;

public record PieceAvailabilityResponse(
        String styleAutoId,
        GarmentSize size,
        Integer availablePieces
) {
}
