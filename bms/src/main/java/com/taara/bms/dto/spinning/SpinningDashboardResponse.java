package com.taara.bms.dto.spinning;

import java.math.BigDecimal;

public record SpinningDashboardResponse(
        BigDecimal totalDispatchedToSpinning,
        BigDecimal totalReceivedFromSpinning,
        BigDecimal netPendingAtFactory
) {
}
