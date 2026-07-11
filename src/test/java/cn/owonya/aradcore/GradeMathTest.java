package cn.owonya.aradcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GradeMathTest {
    @Test
    void scalingAlwaysUsesImmutableBase() {
        double base = 123.4567;
        assertEquals(61.7284, GradeMath.scale(base, 50));
        assertEquals(98.7654, GradeMath.scale(base, 80));
        assertEquals(61.7284, GradeMath.scale(base, 50));
    }

    @Test
    void scalingRoundsToFourDecimals() {
        assertEquals(0.33, GradeMath.scale(1.0, 33));
        assertEquals(100.0, GradeMath.scale(100.0, 100));
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GradeMath.scale(Double.NaN, 50));
        assertThrows(IllegalArgumentException.class, () -> GradeMath.scale(100, 0));
        assertThrows(IllegalArgumentException.class, () -> GradeMath.scale(100, 101));
    }
}
