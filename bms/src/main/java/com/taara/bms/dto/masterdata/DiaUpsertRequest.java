package com.taara.bms.dto.masterdata;

import jakarta.validation.constraints.NotBlank;

public record DiaUpsertRequest(
        @NotBlank(message = "Dia value is required")
        String diaValue
) {
}
