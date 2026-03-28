package com.taara.bms.dto.inhouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CuttingCreateRequest(
        @NotNull(message = "Cutting date is required")
        LocalDate cuttingDate,
        @NotEmpty(message = "At least one cutting row is required")
        List<@Valid CuttingRowRequest> rows,
        String notes
) {
}
