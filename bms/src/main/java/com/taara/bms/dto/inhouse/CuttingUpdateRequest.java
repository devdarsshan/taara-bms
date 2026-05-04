package com.taara.bms.dto.inhouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CuttingUpdateRequest(
        @NotEmpty(message = "At least one cutting row is required")
        List<@Valid CuttingRowRequest> rows,
        @NotNull(message = "Total output pieces is required")
        Integer totalOutputPieces,
        String notes
) {
}
