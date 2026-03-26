package com.schedule.app.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机完成后重新 {@link AlarmScheduler#scheduleAllAlarms(Context)}，避免重启后闹钟丢失。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmScheduler.scheduleAllAlarms(context);
        }
    }
}
