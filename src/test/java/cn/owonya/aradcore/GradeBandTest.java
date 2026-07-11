package cn.owonya.aradcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeBandTest {
    @Test
    void sizeIncludesBothBounds() {
        assertEquals(19, new GradeBand(1, 19, 8).size());
        assertEquals(6, new GradeBand(95, 100, 3).size());
    }
}
