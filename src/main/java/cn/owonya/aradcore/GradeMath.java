package cn.owonya.aradcore;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class GradeMath {
    private GradeMath() {
    }

    static double scale(double base, int percent) {
        if (!Double.isFinite(base)) throw new IllegalArgumentException("base value is not finite");
        if (percent < 1 || percent > 100) throw new IllegalArgumentException("percent must be within 1..100");
        return BigDecimal.valueOf(base)
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
