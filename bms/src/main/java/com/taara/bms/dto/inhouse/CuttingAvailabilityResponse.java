package com.taara.bms.dto.inhouse;

import java.math.BigDecimal;

public record CuttingAvailabilityResponse(
        String diaAutoId,
        String styleAutoId,
        BigDecimal availableQuantityKgs
) {
}
