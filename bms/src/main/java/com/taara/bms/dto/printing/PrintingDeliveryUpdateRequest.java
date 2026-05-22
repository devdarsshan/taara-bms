package com.taara.bms.dto.printing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PrintingDeliveryUpdateRequest(
        @NotNull(message = "Delivery date is required")
        LocalDate deliveryDate,
        @NotBlank(message = "Printing order is required")
        String printingOrderAutoId,
        @NotNull(message = "Pieces delivered is required")
        @Min(value = 1, message = "Pieces delivered must be positive")
        Integer piecesDelivered
) {
}
