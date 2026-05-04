package com.taara.bms.dto.stitching;

import com.taara.bms.enums.GarmentSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record StitchingOrderUpdateRequest(
        @NotNull(message = "Order date is required")
        LocalDate orderDate,
        @NotNull(message = "Expected size is required")
        GarmentSize expectedSize,
        @NotNull(message = "Expected pieces is required")
        @Min(value = 1, message = "Expected pieces must be positive")
        Integer expectedPieces,
        String notes,
        @NotEmpty(message = "At least one row is required")
        List<@Valid StitchingOrderRowRequest> rows
) {
}
