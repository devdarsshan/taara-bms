package com.taara.bms.dto.packing;

import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PackingUpdateRequest(
        @NotNull(message = "Packing date is required")
        LocalDate packingDate,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Size is required")
        GarmentSize size,
        @NotNull(message = "Stock type is required")
        PackingStockType stockType,
        @NotNull(message = "Correctly packed pieces is required")
        @Min(value = 0, message = "Correctly packed pieces cannot be negative")
        Integer correctlyPackedPieces,
        @NotNull(message = "Defective pieces is required")
        @Min(value = 0, message = "Defective pieces cannot be negative")
        Integer defectivePieces
) {
}
