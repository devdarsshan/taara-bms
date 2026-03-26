package com.taara.bms.dto.printing;

public record PrintingDashboardResponse(
        int totalPiecesInPrinting,
        int deliveredPiecesFromPrinting,
        long pendingOrdersCount,
        int defectivePiecesFinalized
) {
}
