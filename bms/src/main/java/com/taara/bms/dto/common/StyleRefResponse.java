package com.taara.bms.dto.common;

import java.util.UUID;

public record StyleRefResponse(
        UUID id,
        String autoId,
        String styleName
) {
}
