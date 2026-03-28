package com.taara.bms.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BigDecimalUtils {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private BigDecimalUtils() {
    }

    public static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
