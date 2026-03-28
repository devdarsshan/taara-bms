package com.taara.bms.dto.stitching;

import com.taara.bms.enums.GarmentSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record StitchingOrderCreateRequest(
        @NotNull(message = "Order date is required")
        LocalDate orderDate,
        @NotNull(message = "Expected size is required")
        GarmentSize expectedSize,
        @NotNull(message = "Expected pcs is required")
        @Min(value = 1, message = "Expected pcs must be positive")
        Integer expectedPieces,
        @NotEmpty(message = "At least one row is required")
        List<@Valid StitchingOrderRowRequest> rows,
        String notes
) {
}
