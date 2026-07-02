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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 课程提醒调度器：按课程时间注册精确闹钟，避免周期轮询造成课前提醒不准。
 */
public class AlarmScheduler {

    private static final int ALARM_BASE_REQUEST_CODE = 10000;
    /** 预留足够槽位取消旧闹钟：两周课程 × 上/下课提醒 × 余量。 */
    private static final int MAX_ALARM_SLOTS = 4096;
    private static final String KEY_SCHEDULED_REQUEST_CODES = "scheduled_course_alarm_request_codes";
    private static final String KEY_LEGACY_ALARM_CLEANUP_DONE = "legacy_alarm_cleanup_done";

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
        boolean beforeClassEnabled = prefs.getBoolean("notify_enabled", true);
        boolean afterClassEnabled = prefs.getBoolean("after_class_notify_enabled", false);
        int beforeMinutes = prefs.getInt("before_class_reminder_minutes", 10);

        cancelAllAlarms(appContext);
        if (!beforeClassEnabled && !afterClassEnabled) {
            return;
        }

        String semesterStart = prefs.getString("semester_start_date", "");
        if (semesterStart.isEmpty()) {
            return;
        }

        LocalDate startDate;
        try {
            startDate = LocalDate.parse(semesterStart);
        } catch (Exception e) {
            return;
        }

        int currentWeek = ReminderTimeCalculator.calculateCurrentWeek(
                startDate, LocalDate.now(), ScheduleConstants.MAX_TEACHING_WEEK);
        if (currentWeek < 1 || currentWeek > ScheduleConstants.MAX_TEACHING_WEEK) {
            return;
        }

        List<Course> courses = AppDatabase.getInstance(appContext)
                .courseDao().getAllCoursesSync();
        int requestCode = ALARM_BASE_REQUEST_CODE;
        Set<String> registeredRequestCodes = new LinkedHashSet<>();

        for (int weekOffset = 0; weekOffset <= 1; weekOffset++) {
            int targetWeek = currentWeek + weekOffset;
            if (targetWeek > ScheduleConstants.MAX_TEACHING_WEEK) {
                continue;
            }

            for (Course course : courses) {
                if (!course.isActiveInWeek(targetWeek)) {
                    continue;
                }

                LocalDate courseDate = ReminderTimeCalculator.courseDateForWeek(
                        startDate, targetWeek, course.getDayOfWeek());

                if (beforeClassEnabled) {
                    LocalDateTime reminderTime = ReminderTimeCalculator.beforeClassTime(courseDate,
                            SectionTimeMapper.getStartHour(appContext, course.getStartSection()),
                            SectionTimeMapper.getStartMinute(appContext, course.getStartSection()),
                            beforeMinutes);
                    if (reminderTime.isAfter(LocalDateTime.now()) && requestCode < nextSlotLimit()) {
                        String time = SectionTimeMapper.getSectionRangeDisplay(
                                appContext, course.getStartSection(), course.getEndSection());
                        if (scheduleAlarm(appContext, requestCode, reminderTime,
                                course, time, "即将上课", "还有 " + beforeMinutes + " 分钟上课")) {
                            registeredRequestCodes.add(String.valueOf(requestCode));
                        }
                        requestCode++;
                    }
                }

                if (afterClassEnabled) {
                    int[] end = ReminderTimeCalculator.parseHourMinute(
                            SectionTimeMapper.getEndTime(appContext, course.getEndSection()));
                    LocalDateTime classEnd = courseDate.atTime(end[0], end[1]);
                    if (classEnd.isAfter(LocalDateTime.now()) && requestCode < nextSlotLimit()) {
                        String time = SectionTimeMapper.getSectionRangeDisplay(
                                appContext, course.getStartSection(), course.getEndSection());
                        if (scheduleAlarm(appContext, requestCode, classEnd,
                                course, time, "下课提醒", "课程已结束")) {
                            registeredRequestCodes.add(String.valueOf(requestCode));
                        }
                        requestCode++;
                    }
                }
            }
        }
        saveScheduledRequestCodes(appContext, registeredRequestCodes);
    }

    /** 取消本应用登记在 {@link CourseAlarmReceiver} 上的全部课程提醒。 */
    public static void cancelAllAlarms(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        Set<String> requestCodes = new HashSet<>(
                prefs.getStringSet(KEY_SCHEDULED_REQUEST_CODES, new HashSet<>()));
        for (String requestCodeText : requestCodes) {
            try {
                cancelAlarmByRequestCode(appContext, alarmManager, Integer.parseInt(requestCodeText));
            } catch (NumberFormatException ignored) {
            }
        }
        prefs.edit().remove(KEY_SCHEDULED_REQUEST_CODES).apply();

        // 从旧版本升级时没有持久化 requestCode，保留一次全槽位清理避免历史闹钟残留。
        if (!prefs.getBoolean(KEY_LEGACY_ALARM_CLEANUP_DONE, false)) {
            for (int i = 0; i < MAX_ALARM_SLOTS; i++) {
                cancelAlarmByRequestCode(appContext, alarmManager, ALARM_BASE_REQUEST_CODE + i);
            }
            prefs.edit().putBoolean(KEY_LEGACY_ALARM_CLEANUP_DONE, true).apply();
        }
    }

    private static int nextSlotLimit() {
        return ALARM_BASE_REQUEST_CODE + MAX_ALARM_SLOTS;
    }

    private static boolean scheduleAlarm(Context context, int requestCode, LocalDateTime triggerTime,
                                      Course course, String time, String title, String message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        Intent intent = new Intent(context, CourseAlarmReceiver.class);
        intent.putExtra("notification_id", requestCode);
        intent.putExtra("course_id", course.getId());
        intent.putExtra("course_name", course.getCourseName());
        intent.putExtra("location", course.getLocation());
        intent.putExtra("time", time);
        intent.putExtra("notification_title", title);
        intent.putExtra("notification_message", message);

        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
        }
        return true;
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    private static void saveScheduledRequestCodes(Context context, Set<String> requestCodes) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putStringSet(KEY_SCHEDULED_REQUEST_CODES, new LinkedHashSet<>(requestCodes))
                .apply();
    }

    private static void cancelAlarmByRequestCode(Context context, AlarmManager alarmManager, int requestCode) {
        Intent intent = new Intent(context, CourseAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode,
                intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }
}
