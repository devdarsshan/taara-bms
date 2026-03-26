package com.taara.bms.dto.masterdata;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record StyleUpsertRequest(
        @NotBlank(message = "Style name is required")
        String styleName,
        List<@NotBlank(message = "Color is required") String> colors
) {
}
