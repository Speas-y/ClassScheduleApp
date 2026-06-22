package com.schedule.app.ui.course;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 测试周次重叠判断逻辑（纯逻辑，不依赖 Android）。
 */
public class WeeksOverlapTest {

    private boolean weeksOverlap(int startW1, int endW1, int type1,
                                 int startW2, int endW2, int type2) {
        if (startW1 > endW2 || startW2 > endW1) return false;
        if (type1 == 0 && type2 == 0) return true;
        if (type1 == 0 || type2 == 0) return true;
        if (type1 == type2) return true;
        return false;
    }

    @Test
    public void bothEveryWeek_overlap() {
        assertTrue(weeksOverlap(1, 20, 0, 5, 15, 0));
    }

    @Test
    public void sameOddWeek_overlap() {
        assertTrue(weeksOverlap(1, 20, 1, 5, 15, 1));
    }

    @Test
    public void sameEvenWeek_overlap() {
        assertTrue(weeksOverlap(1, 20, 2, 5, 15, 2));
    }

    @Test
    public void oddVsEven_noOverlap() {
        assertFalse(weeksOverlap(1, 20, 1, 5, 15, 2));
    }

    @Test
    public void everyWeekVsOdd_overlap() {
        assertTrue(weeksOverlap(1, 20, 0, 5, 15, 1));
    }

    @Test
    public void everyWeekVsEven_overlap() {
        assertTrue(weeksOverlap(1, 20, 0, 5, 15, 2));
    }

    @Test
    public void weekRangeNoIntersection_noOverlap() {
        assertFalse(weeksOverlap(1, 5, 0, 6, 10, 0));
    }

    @Test
    public void weekRangeTouching_overlap() {
        assertTrue(weeksOverlap(1, 5, 0, 5, 10, 0));
    }

    @Test
    public void singleWeekSameWeek_overlap() {
        assertTrue(weeksOverlap(3, 3, 0, 3, 3, 0));
    }
}
