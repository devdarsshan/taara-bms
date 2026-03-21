package com.taara.bms.dto.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StyleUpsertRequest(
        @NotBlank(message = "Style name is required")
        String styleName,
        @NotEmpty(message = "At least one color is required")
        List<@NotBlank(message = "Color is required") String> colors
) {
}
