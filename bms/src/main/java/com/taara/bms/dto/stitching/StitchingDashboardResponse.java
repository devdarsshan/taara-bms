package com.taara.bms.dto.stitching;

import com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse;
import java.util.List;

public record StitchingDashboardResponse(
        int totalPiecesInStitching,
        int piecesDeliveredFromStitching,
        long pendingOrdersCount,
        long partiallyDeliveredOrdersCount,
        int defectivePiecesFinalized,
        int readyToStitchPieces,
        List<ReadyToStitchBreakdownResponse> readyToStitchBreakdown,
        List<SectionPendingPiecesResponse> ordersBySection
) {
}
