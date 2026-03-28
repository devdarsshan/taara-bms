package com.taara.bms.dto.stitching;

import com.taara.bms.enums.GarmentSize;

public record StitchingAvailabilityResponse(
        String styleAutoId,
        GarmentSize size,
        Integer availablePieces
) {
}
