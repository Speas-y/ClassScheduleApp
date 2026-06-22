package com.schedule.app.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.schedule.app.MainActivity;
import com.schedule.app.R;
import com.schedule.app.ScheduleApplication;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.data.entity.Course;
import com.schedule.app.util.ScheduleConstants;
import com.schedule.app.util.SectionTimeMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 后台Worker，定期检查课程时间并发送提醒通知。
 * 替代原来的AlarmManager实现，更加轻量和可靠。
 */
public class ReminderWorker extends Worker {

    private static final int NOTIFICATION_ID_BASE = 20000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean beforeClassEnabled = prefs.getBoolean("notify_enabled", true);
        boolean afterClassEnabled = prefs.getBoolean("after_class_notify_enabled", false);

        if (!beforeClassEnabled && !afterClassEnabled) {
            return Result.success();
        }

        List<Course> courses = CourseRepository.getInstance((android.app.Application) context.getApplicationContext())
                .getAllCoursesSync();

        LocalDateTime now = LocalDateTime.now();
        int currentDayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        // 计算当前周次
        String semesterStart = prefs.getString("semester_start_date", "");
        if (semesterStart.isEmpty()) {
            return Result.success();
        }

        try {
            LocalDate startDate = LocalDate.parse(semesterStart);
            long daysBetween = ChronoUnit.DAYS.between(startDate, now.toLocalDate());
            int currentWeek = (int) (daysBetween / 7) + 1;

            if (currentWeek < 1 || currentWeek > ScheduleConstants.MAX_TEACHING_WEEK) {
                return Result.success();
            }

            for (Course course : courses) {
                if (course.getDayOfWeek() != currentDayOfWeek) continue;
                if (!course.isActiveInWeek(currentWeek)) continue;

                // 检查上课提醒
                if (beforeClassEnabled) {
                    checkBeforeClassReminder(context, course, now);
                }

                // 检查下课提醒
                if (afterClassEnabled) {
                    checkAfterClassReminder(context, course, now);
                }
            }
        } catch (Exception e) {
            return Result.failure();
        }

        return Result.success();
    }

    private void checkBeforeClassReminder(Context context, Course course, LocalDateTime now) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int reminderMinutes = prefs.getInt("before_class_reminder_minutes", 10);

        int startHour = SectionTimeMapper.getStartHour(context, course.getStartSection());
        int startMinute = SectionTimeMapper.getStartMinute(context, course.getStartSection());

        LocalTime classStartTime = LocalTime.of(startHour, startMinute);
        LocalDateTime classStartDateTime = now.toLocalDate().atTime(classStartTime);

        // 计算提醒时间（上课前N分钟）
        LocalDateTime reminderTime = classStartDateTime.minusMinutes(reminderMinutes);

        // 检查是否在提醒时间窗口内（当前时间在提醒时间到上课时间之间）
        if (now.isAfter(reminderTime) && now.isBefore(classStartDateTime)) {
            sendNotification(context, course, "即将上课",
                    "还有 " + reminderMinutes + " 分钟上课", course.getStartSection());
        }
    }

    private void checkAfterClassReminder(Context context, Course course, LocalDateTime now) {
        // 使用结束节的结束时间，而非开始时间+duration
        String endTimeStr = SectionTimeMapper.getEndTime(context, course.getEndSection());
        int[] endParsed = parseHHmm(endTimeStr);
        LocalTime classEndTime = LocalTime.of(endParsed[0], endParsed[1]);
        LocalDateTime classEndDateTime = now.toLocalDate().atTime(classEndTime);

        // 检查是否在下课时间附近（5分钟内）
        LocalDateTime fiveMinutesAfter = classEndDateTime.plusMinutes(5);
        if (now.isAfter(classEndDateTime) && now.isBefore(fiveMinutesAfter)) {
            sendNotification(context, course, "下课提醒",
                    "课程已结束", course.getEndSection() + 10000);
        }
    }

    private void sendNotification(Context context, Course course, String title, String message, int notificationId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ScheduleApplication.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title + " - " + course.getCourseName())
                .setContentText(message + "\n" + course.getLocation())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + notificationId, builder.build());
        }
    }

    private static int[] parseHHmm(String time) {
        try {
            if (time != null && time.contains(":")) {
                String[] parts = time.trim().split(":");
                if (parts.length >= 2) {
                    int h = Integer.parseInt(parts[0].trim());
                    int m = Integer.parseInt(parts[1].trim());
                    if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                        return new int[]{h, m};
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return new int[]{0, 0};
    }
}
