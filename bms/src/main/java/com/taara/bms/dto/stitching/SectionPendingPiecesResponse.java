package com.taara.bms.dto.stitching;

import com.taara.bms.dto.common.StitchingSectionRefResponse;

public record SectionPendingPiecesResponse(
        StitchingSectionRefResponse section,
        int pendingPieces
) {
}
