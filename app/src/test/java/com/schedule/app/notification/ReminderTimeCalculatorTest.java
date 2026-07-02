package com.schedule.app.notification;

import com.schedule.app.util.ScheduleConstants;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** 提醒时间计算纯逻辑测试。 */
public class ReminderTimeCalculatorTest {

    @Test
    public void calculateCurrentWeek_clampsBeforeSemesterToFirstWeek() {
        LocalDate semesterStart = LocalDate.of(2026, 3, 2);
        LocalDate today = LocalDate.of(2026, 2, 20);

        int week = ReminderTimeCalculator.calculateCurrentWeek(
                semesterStart, today, ScheduleConstants.MAX_TEACHING_WEEK);

        assertEquals(1, week);
    }

    @Test
    public void calculateCurrentWeek_clampsAfterMaxWeek() {
        LocalDate semesterStart = LocalDate.of(2026, 3, 2);
        LocalDate today = semesterStart.plusWeeks(40);

        int week = ReminderTimeCalculator.calculateCurrentWeek(
                semesterStart, today, ScheduleConstants.MAX_TEACHING_WEEK);

        assertEquals(ScheduleConstants.MAX_TEACHING_WEEK, week);
    }

    @Test
    public void courseDateForWeek_usesCurrentTeachingWeekRange() {
        LocalDate semesterStart = LocalDate.of(2026, 3, 2);

        LocalDate courseDate = ReminderTimeCalculator.courseDateForWeek(semesterStart, 2, 3);

        assertEquals(LocalDate.of(2026, 3, 11), courseDate);
    }

    @Test
    public void beforeClassTime_subtractsConfiguredMinutes() {
        LocalDate courseDate = LocalDate.of(2026, 3, 11);

        LocalDateTime reminder = ReminderTimeCalculator.beforeClassTime(courseDate, 8, 0, 15);

        assertEquals(LocalDateTime.of(2026, 3, 11, 7, 45), reminder);
    }

    @Test
    public void parseHourMinute_invalidTimeFallsBackToEightOClock() {
        assertArrayEquals(new int[]{8, 0}, ReminderTimeCalculator.parseHourMinute("25:99"));
    }
}
