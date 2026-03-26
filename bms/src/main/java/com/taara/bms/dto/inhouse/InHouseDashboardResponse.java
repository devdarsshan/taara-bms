package com.taara.bms.dto.inhouse;

import java.math.BigDecimal;

public record InHouseDashboardResponse(
        BigDecimal totalFabricInStock,
        long cuttingInProgressCount,
        int totalPiecesCut,
        int readyToStitchPieces,
        int stitchedPlainStockTotal,
        int printedStockTotal,
        int defectiveStockTotal
) {
}
