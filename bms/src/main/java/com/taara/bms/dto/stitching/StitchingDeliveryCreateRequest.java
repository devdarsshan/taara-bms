package com.taara.bms.dto.stitching;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record StitchingDeliveryCreateRequest(
        @NotNull(message = "Delivery date is required")
        LocalDate deliveryDate,
        @NotBlank(message = "Stitching order AutoId is required")
        String stitchingOrderAutoId,
        @NotNull(message = "Pieces delivered is required")
        @Min(value = 1, message = "Pieces delivered must be positive")
        Integer piecesDelivered,
        boolean overrideWarnings
) {
}
