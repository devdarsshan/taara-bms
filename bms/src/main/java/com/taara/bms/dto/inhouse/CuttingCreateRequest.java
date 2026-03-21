package com.taara.bms.dto.inhouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CuttingCreateRequest(
        @NotNull(message = "Cutting date is required")
        LocalDate cuttingDate,
        @NotBlank(message = "Dia AutoId is required")
        String diaAutoId,
        @NotBlank(message = "Style AutoId is required")
        String styleAutoId,
        @NotNull(message = "Quantity used is required")
        @DecimalMin(value = "0.01", message = "Quantity used must be positive")
        BigDecimal quantityUsedKgs,
        @Min(value = 1, message = "Output pieces must be positive")
        Integer outputPieces,
        String notes
) {
}
