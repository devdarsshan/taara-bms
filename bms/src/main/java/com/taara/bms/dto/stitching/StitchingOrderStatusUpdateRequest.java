package com.taara.bms.dto.stitching;

import com.taara.bms.enums.StitchingOrderStatus;
import jakarta.validation.constraints.NotNull;

public record StitchingOrderStatusUpdateRequest(
        @NotNull(message = "Status is required")
        StitchingOrderStatus status
) {
}
