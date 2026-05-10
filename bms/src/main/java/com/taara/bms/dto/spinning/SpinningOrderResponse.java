package com.taara.bms.dto.spinning;

import com.taara.bms.dto.common.StitchingSectionRefResponse;
import com.taara.bms.dto.common.StyleRefResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SpinningOrderResponse(
        UUID id,
        String autoId,
        LocalDate dispatchDate,
        UUID linkedYarnOrderId,
        String linkedYarnOrderAutoId,
        StyleRefResponse style,
        BigDecimal quantitySentKgs,
        StitchingSectionRefResponse stitchingSection,
        String factoryNotes,
        boolean autoCreated,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
