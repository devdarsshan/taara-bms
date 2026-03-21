package com.taara.bms.dto.yarn;

import java.math.BigDecimal;

public record YarnDashboardResponse(
        BigDecimal totalYarnOrdered,
        BigDecimal yarnInOrder,
        BigDecimal yarnDispatchedToSpinning
) {
}
