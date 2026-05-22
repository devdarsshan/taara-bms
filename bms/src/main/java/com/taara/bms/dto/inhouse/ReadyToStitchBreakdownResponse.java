package com.taara.bms.dto.inhouse;

import com.taara.bms.enums.GarmentSize;

public record ReadyToStitchBreakdownResponse(
        GarmentSize size,
        int totalPieces
) {
}
