package com.schedule.app.notification;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * {@link AlarmManager} 触发的广播：读出 Intent 中的课程信息并调用 {@link NotificationHelper}。
 */
public class CourseAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int courseId = intent.getIntExtra("course_id", 0);
        int notificationId = intent.getIntExtra("notification_id", courseId);
        String courseName = intent.getStringExtra("course_name");
        String location = intent.getStringExtra("location");
        String time = intent.getStringExtra("time");
        String title = intent.getStringExtra("notification_title");
        String message = intent.getStringExtra("notification_message");

        if (courseName == null) courseName = "未知课程";
        if (location == null) location = "";
        if (time == null) time = "";
        if (title == null) title = "课程提醒";
        if (message == null) message = "";

        NotificationHelper.showCourseReminder(context, notificationId, courseName, location, time, title, message);
    }
}
