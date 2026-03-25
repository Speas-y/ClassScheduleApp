package com.schedule.app.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CourseAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int courseId = intent.getIntExtra("course_id", 0);
        String courseName = intent.getStringExtra("course_name");
        String location = intent.getStringExtra("location");
        String time = intent.getStringExtra("time");

        if (courseName == null) courseName = "未知课程";
        if (location == null) location = "";
        if (time == null) time = "";

        NotificationHelper.showCourseReminder(context, courseId, courseName, location, time);
    }
}
