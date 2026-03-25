package com.schedule.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class ScheduleApplication extends Application {

    public static final String CHANNEL_ID = "course_reminder_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("在上课前10分钟提醒您");
        channel.enableVibration(true);
        channel.setShowBadge(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
