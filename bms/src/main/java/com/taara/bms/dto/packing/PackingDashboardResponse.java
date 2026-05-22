package com.taara.bms.dto.packing;

import com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse;
import java.util.List;

public record PackingDashboardResponse(
        int totalPackedPieces,
        int totalDefectivePieces,
        int totalStitchedPlainPieces,
        int totalPrintedPieces,
        List<ReadyToStitchBreakdownResponse> stitchedPlainBreakdown,
        List<ReadyToStitchBreakdownResponse> printedBreakdown
) {
}
