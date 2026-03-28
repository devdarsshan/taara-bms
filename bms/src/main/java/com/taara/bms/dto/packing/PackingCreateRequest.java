package com.taara.bms.dto.packing;

import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PackingCreateRequest(
        @NotNull(message = "Packing date is required")
        LocalDate packingDate,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Size is required")
        GarmentSize size,
        @NotNull(message = "Stock type is required")
        PackingStockType stockType,
        @NotNull(message = "Correctly packed pcs is required")
        @Min(value = 0, message = "Correctly packed pcs cannot be negative")
        Integer correctlyPackedPieces,
        @NotNull(message = "Defective pcs is required")
        @Min(value = 0, message = "Defective pcs cannot be negative")
        Integer defectivePieces
) {
}
