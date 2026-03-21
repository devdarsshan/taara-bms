package com.taara.bms.dto.common;

import java.util.UUID;

public record DiaRefResponse(
        UUID id,
        String autoId,
        String diaValue
) {
}
