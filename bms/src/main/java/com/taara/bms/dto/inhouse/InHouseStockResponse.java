package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.DiaRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import java.math.BigDecimal;

public record InHouseStockResponse(
        DiaRefResponse dia,
        StyleRefResponse style,
        BigDecimal availableQuantityKgs
) {
}
