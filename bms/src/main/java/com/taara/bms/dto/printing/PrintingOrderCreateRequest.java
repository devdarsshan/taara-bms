package com.taara.bms.dto.printing;

import com.taara.bms.enums.GarmentSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PrintingOrderCreateRequest(
        @NotNull(message = "Order date is required")
        LocalDate orderDate,
        @NotBlank(message = "Section is required")
        String printingSectionAutoId,
        @NotBlank(message = "Style is required")
        String styleAutoId,
        @NotNull(message = "Size is required")
        GarmentSize size,
        @NotNull(message = "Pieces ordered is required")
        @Min(value = 1, message = "Pieces ordered must be positive")
        Integer piecesOrdered,
        String notes
) {
}
