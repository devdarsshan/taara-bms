package com.taara.bms.dto.spinning;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SpinningDeliveryCreateRequest(
        @NotNull(message = "Delivery date is required")
        LocalDate deliveryDate,
        @NotBlank(message = "Style AutoId is required")
        String styleAutoId,
        String stitchingSectionAutoId,
        @NotNull(message = "Actual quantity is required")
        @DecimalMin(value = "0.01", message = "Actual quantity must be positive")
        BigDecimal actualQuantityKgs,
        BigDecimal bufferQuantityKgs,
        @DecimalMin(value = "0.00", message = "Price per kg cannot be negative")
        BigDecimal pricePerKg,
        @DecimalMin(value = "0.00", message = "Paid amount cannot be negative")
        BigDecimal paidAmount,
        String notes
) {
}
