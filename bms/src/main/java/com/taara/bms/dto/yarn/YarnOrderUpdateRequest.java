package com.taara.bms.dto.yarn;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record YarnOrderUpdateRequest(
        @NotNull(message = "Order date is required")
        LocalDate orderDate,

        @NotBlank(message = "Style is required")
        String styleAutoId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
        BigDecimal quantityKgs,

        String supplierNotes
) {
}
