package com.taara.bms.dto.printing;

import com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse;
import java.util.List;

public record PrintingDashboardResponse(
        int totalPiecesInPrinting,
        int deliveredPiecesFromPrinting,
        long pendingOrdersCount,
        int defectivePiecesFinalized,
        int stitchedStockPieces,
        List<ReadyToStitchBreakdownResponse> stitchedStockBreakdown
) {
}
