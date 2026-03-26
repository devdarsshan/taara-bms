package com.taara.bms.dto.packing;

public record PackingDashboardResponse(
        int totalPackedPieces,
        int totalDefectivePieces
) {
}
