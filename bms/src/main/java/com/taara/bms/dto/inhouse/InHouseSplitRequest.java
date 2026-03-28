package com.taara.bms.dto.inhouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InHouseSplitRequest(
        @NotBlank(message = "Dia AutoId is required")
        String diaAutoId,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be positive")
        BigDecimal quantityKgs
) {
}
