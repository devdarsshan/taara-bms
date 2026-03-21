package com.taara.bms.dto.masterdata;

import com.taara.bms.enums.StitchingSectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StitchingSectionUpsertRequest(
        @NotBlank(message = "Section name is required")
        String sectionName,
        @NotNull(message = "Section type is required")
        StitchingSectionType type
) {
}
