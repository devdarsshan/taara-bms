package com.taara.bms.dto.inhouse;

import com.taara.bms.enums.GarmentSize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CuttingRowRequest(
        @NotBlank(message = "Dia is required")
        String diaAutoId,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Size is required")
        GarmentSize size,
        @NotNull(message = "Quantity used is required")
        @DecimalMin(value = "0.01", message = "Quantity used must be positive")
        BigDecimal quantityUsedKgs,
        @Min(value = 0, message = "Output pieces cannot be negative")
        Integer outputPieces
) {
}
