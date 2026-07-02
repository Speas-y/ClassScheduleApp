package com.schedule.app.notification;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** 课程提醒时间的纯逻辑计算，便于单元测试覆盖边界。 */
public final class ReminderTimeCalculator {

    private ReminderTimeCalculator() {
    }

    public static int calculateCurrentWeek(LocalDate semesterStart, LocalDate today, int maxWeek) {
        long daysBetween = ChronoUnit.DAYS.between(semesterStart, today);
        int week = (int) (daysBetween / 7) + 1;
        return Math.max(1, Math.min(week, maxWeek));
    }

    public static LocalDate courseDateForWeek(LocalDate semesterStart, int teachingWeek, int dayOfWeek) {
        return semesterStart
                .plusWeeks(teachingWeek - 1L)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)));
    }

    public static LocalDateTime beforeClassTime(LocalDate courseDate,
                                                int startHour,
                                                int startMinute,
                                                int beforeMinutes) {
        return courseDate.atTime(startHour, startMinute).minusMinutes(beforeMinutes);
    }

    public static int[] parseHourMinute(String time) {
        try {
            String[] parts = time.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return new int[]{hour, minute};
            }
        } catch (Exception ignored) {
        }
        return new int[]{8, 0};
    }
}
