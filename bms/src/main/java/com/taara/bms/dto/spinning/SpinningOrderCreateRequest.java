package com.taara.bms.dto.spinning;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SpinningOrderCreateRequest(
        @NotNull(message = "Dispatch date is required")
        LocalDate dispatchDate,
        String linkedYarnOrderAutoId,
        @NotBlank(message = "Style AutoId is required")
        String styleAutoId,
        @NotNull(message = "Quantity sent is required")
        @DecimalMin(value = "0.01", message = "Quantity sent must be positive")
        BigDecimal quantitySentKgs,
        String factoryNotes
) {
}
