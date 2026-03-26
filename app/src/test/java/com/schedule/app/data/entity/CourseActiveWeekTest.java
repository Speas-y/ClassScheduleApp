package com.schedule.app.data.entity;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link Course#isActiveInWeek(int)} 周次与单双周语义。 */
public class CourseActiveWeekTest {

    private static Course course(int startW, int endW, int weekType) {
        return new Course("微积分", "老师", "教室1",
                1, 1, 1, startW, endW, weekType, 0xFF000000);
    }

    @Test
    public void everyWeek_inRange_isActive() {
        Course c = course(1, 20, 0);
        assertTrue(c.isActiveInWeek(1));
        assertTrue(c.isActiveInWeek(10));
        assertTrue(c.isActiveInWeek(20));
    }

    @Test
    public void everyWeek_outsideRange_notActive() {
        Course c = course(3, 10, 0);
        assertFalse(c.isActiveInWeek(2));
        assertFalse(c.isActiveInWeek(11));
    }

    @Test
    public void oddWeek_onlyOddInRange() {
        Course c = course(1, 10, 1);
        assertTrue(c.isActiveInWeek(1));
        assertTrue(c.isActiveInWeek(3));
        assertFalse(c.isActiveInWeek(2));
        assertFalse(c.isActiveInWeek(4));
    }

    @Test
    public void evenWeek_onlyEvenInRange() {
        Course c = course(1, 10, 2);
        assertFalse(c.isActiveInWeek(1));
        assertTrue(c.isActiveInWeek(2));
        assertFalse(c.isActiveInWeek(3));
        assertTrue(c.isActiveInWeek(4));
    }
}
