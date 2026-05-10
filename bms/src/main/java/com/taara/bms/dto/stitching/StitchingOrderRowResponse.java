package com.taara.bms.dto.stitching;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import java.util.UUID;

public record StitchingOrderRowResponse(
        UUID id,
        StitchingSectionRefResponse stitchingSection,
        StyleRefResponse style,
        GarmentSize size,
        Integer piecesTaken,
        java.math.BigDecimal ratePerPiece,
        java.math.BigDecimal totalPrice
) {
}
