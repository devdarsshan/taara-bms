package com.taara.bms.dto.stitching;

import com.taara.bms.enums.GarmentSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StitchingOrderRowRequest(
        @NotBlank(message = "Section is required")
        String stitchingSectionAutoId,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Size is required")
        GarmentSize size,
        @NotNull(message = "Pieces taken is required")
        @Min(value = 1, message = "Pieces taken must be positive")
        Integer piecesTaken,
        java.math.BigDecimal ratePerPiece
) {
}
