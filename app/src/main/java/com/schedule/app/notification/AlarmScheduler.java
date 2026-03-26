package com.schedule.app.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import com.schedule.app.data.db.AppDatabase;
import com.schedule.app.data.entity.Course;
import com.schedule.app.util.ScheduleConstants;
import com.schedule.app.util.SectionTimeMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 课前提醒：按课程设置与学期首日，为「当前周 + 下一周」内尚未发生的课，
 * 在上课开始前 10 分钟通过 {@link AlarmManager} 触发 {@link CourseAlarmReceiver}。
 * Android 12+ 若无精确闹钟权限则降级为非精确。
 */
public class AlarmScheduler {

    private static final int REMINDER_MINUTES_BEFORE = 10;
    private static final int ALARM_BASE_REQUEST_CODE = 10000;
    /** 单次登记最多占用连续 requestCode 个数；需 ≥ 历史单次最大课数×2，并预留余量。 */
    private static final int MAX_ALARM_SLOTS = 512;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AlarmScheduler");
        t.setDaemon(true);
        return t;
    });

    public static void scheduleAllAlarms(Context context) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> scheduleAllAlarmsSync(appContext));
    }

    private static void scheduleAllAlarmsSync(Context appContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        boolean notifyEnabled = prefs.getBoolean("notify_enabled", true);
        if (!notifyEnabled) {
            cancelAllAlarms(appContext);
            return;
        }

        String semesterStart = prefs.getString("semester_start_date", "");
        if (semesterStart.isEmpty()) {
            cancelAllAlarms(appContext);
            return;
        }

        LocalDate startDate;
        try {
            startDate = LocalDate.parse(semesterStart);
        } catch (Exception e) {
            cancelAllAlarms(appContext);
            return;
        }

        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(startDate, today);
        int currentWeek = (int) (daysBetween / 7) + 1;
        if (currentWeek < 1) currentWeek = 1;
        if (currentWeek > ScheduleConstants.MAX_TEACHING_WEEK) {
            currentWeek = ScheduleConstants.MAX_TEACHING_WEEK;
        }

        List<Course> courses = AppDatabase.getInstance(appContext)
                .courseDao().getAllCoursesSync();

        cancelAllAlarms(appContext);

        int requestCode = ALARM_BASE_REQUEST_CODE;

        for (int weekOffset = 0; weekOffset <= 1; weekOffset++) {
            int targetWeek = currentWeek + weekOffset;
            if (targetWeek > ScheduleConstants.MAX_TEACHING_WEEK) {
                continue;
            }

            for (Course course : courses) {
                if (!course.isActiveInWeek(targetWeek)) continue;

                LocalDate weekStart = startDate.plusWeeks(targetWeek - 1);
                LocalDate courseDate = weekStart.with(TemporalAdjusters.nextOrSame(
                        DayOfWeek.of(course.getDayOfWeek())));

                int startHour = SectionTimeMapper.getStartHour(appContext, course.getStartSection());
                int startMinute = SectionTimeMapper.getStartMinute(appContext, course.getStartSection());

                LocalDateTime classTime = courseDate.atTime(startHour, startMinute);
                LocalDateTime reminderTime = classTime.minusMinutes(REMINDER_MINUTES_BEFORE);

                if (reminderTime.isBefore(LocalDateTime.now())) continue;

                long triggerMillis = reminderTime.atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli();

                String timeStr = SectionTimeMapper.getSectionRangeDisplay(
                        appContext, course.getStartSection(), course.getEndSection());

                scheduleExactAlarm(appContext, requestCode, triggerMillis,
                        course.getId(), course.getCourseName(),
                        course.getLocation(), timeStr);

                requestCode++;
            }
        }
    }

    /** 取消本应用登记在 {@link CourseAlarmReceiver} 上的所有课前闹钟（关闭提醒或无法计算课历时调用）。 */
    public static void cancelAllAlarms(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < MAX_ALARM_SLOTS; i++) {
            Intent intent = new Intent(appContext, CourseAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(appContext, ALARM_BASE_REQUEST_CODE + i,
                    intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                alarmManager.cancel(pi);
                pi.cancel();
            }
        }
    }

    private static void scheduleExactAlarm(Context context, int requestCode,
                                           long triggerMillis, int courseId,
                                           String courseName, String location, String time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, CourseAlarmReceiver.class);
        intent.putExtra("course_id", courseId);
        intent.putExtra("course_name", courseName);
        intent.putExtra("location", location);
        intent.putExtra("time", time);

        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        }
    }
}
