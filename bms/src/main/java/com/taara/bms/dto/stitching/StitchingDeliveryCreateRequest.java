package com.taara.bms.dto.stitching;

import com.taara.bms.enums.GarmentSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record StitchingDeliveryCreateRequest(
        @NotNull(message = "Delivery date is required")
        LocalDate deliveryDate,
        @NotBlank(message = "Section is required")
        String stitchingSectionAutoId,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Size is required")
        GarmentSize size,
        @NotNull(message = "Pieces delivered is required")
        @Min(value = 1, message = "Pieces delivered must be positive")
        Integer piecesDelivered
) {
}
