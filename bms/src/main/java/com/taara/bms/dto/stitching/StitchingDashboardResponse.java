package com.taara.bms.dto.stitching;

import java.util.List;

public record StitchingDashboardResponse(
        int totalPiecesInStitching,
        int piecesDeliveredFromStitching,
        long pendingOrdersCount,
        long partiallyDeliveredOrdersCount,
        int defectivePiecesFinalized,
        List<SectionPendingPiecesResponse> ordersBySection
) {
}
