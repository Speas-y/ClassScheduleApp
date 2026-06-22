package com.schedule.app.notification;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * 使用WorkManager替代AlarmManager进行课程提醒。
 * 更加轻量、可靠，且不需要精确闹钟权限。
 */
public class AlarmScheduler {

    private static final String WORK_NAME = "course_reminder_work";

    public static void scheduleAllAlarms(Context context) {
        // 取消现有的工作
        cancelAllAlarms(context);
        
        // 创建周期性工作请求，每15分钟检查一次
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                15, TimeUnit.MINUTES)
                .build();
        
        // 添加工作请求，保持现有工作
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }

    public static void cancelAllAlarms(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}
