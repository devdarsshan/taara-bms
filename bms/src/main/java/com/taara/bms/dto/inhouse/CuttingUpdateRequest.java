package com.taara.bms.dto.inhouse;

import jakarta.validation.constraints.Min;

public record CuttingUpdateRequest(
        @Min(value = 1, message = "Output pieces must be positive")
        Integer outputPieces,
        String notes
) {
}
