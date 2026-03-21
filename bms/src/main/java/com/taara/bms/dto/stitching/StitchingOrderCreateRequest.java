package com.taara.bms.dto.stitching;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record StitchingOrderCreateRequest(
        @NotNull(message = "Order date is required")
        LocalDate orderDate,
        @NotBlank(message = "Stitching section AutoId is required")
        String stitchingSectionAutoId,
        @NotBlank(message = "Style AutoId is required")
        String styleAutoId,
        @NotNull(message = "Pieces ordered is required")
        @Min(value = 1, message = "Pieces ordered must be positive")
        Integer piecesOrdered,
        String notes
) {
}
