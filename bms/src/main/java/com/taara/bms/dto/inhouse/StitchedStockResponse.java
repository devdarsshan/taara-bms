package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.StyleRefResponse;
import com.taara.bms.enums.GarmentSize;
import java.time.LocalDate;

public record StitchedStockResponse(
        StyleRefResponse style,
        GarmentSize size,
        Integer plainPieces,
        Integer printedPieces,
        LocalDate latestTransactionDate,
        int defectivePieces
) {
}
