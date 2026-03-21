package com.taara.bms.dto.inhouse;

import com.taara.bms.dto.common.StyleRefResponse;
import java.time.LocalDate;

public record StitchedStockResponse(
        StyleRefResponse style,
        LocalDate latestTransactionDate,
        int goodPieces,
        int defectivePieces
) {
}
