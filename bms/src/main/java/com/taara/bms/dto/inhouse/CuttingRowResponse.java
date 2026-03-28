package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.DiaRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import java.math.BigDecimal;
import java.util.UUID;

public record CuttingRowResponse(
        UUID id,
        DiaRefResponse dia,
        StyleRefResponse style,
        GarmentSize size,
        BigDecimal quantityUsedKgs,
        Integer outputPieces
) {
}
