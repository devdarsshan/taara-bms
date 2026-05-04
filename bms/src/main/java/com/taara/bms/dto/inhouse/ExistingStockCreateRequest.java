package com.taara.bms.dto.inhouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExistingStockCreateRequest(
        @NotNull(message = "Entry date is required")
        LocalDate entryDate,
        @NotBlank(message = "Dia is required")
        String diaAutoId,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be positive")
        BigDecimal quantityKgs,
        String notes
) {
}
