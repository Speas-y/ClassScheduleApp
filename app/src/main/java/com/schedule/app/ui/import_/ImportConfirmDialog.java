package com.schedule.app.ui.import_;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import com.schedule.app.R;
import com.schedule.app.data.entity.Course;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.notification.AlarmScheduler;

import java.util.List;

/** 统一展示导入确认弹窗，并在导入完成后重新调度课程提醒。 */
public final class ImportConfirmDialog {

    private ImportConfirmDialog() {
    }

    public static void show(Activity activity,
                            CourseRepository repository,
                            ImportResult result,
                            Runnable onComplete,
                            Runnable onCancel) {
        List<Course> courses = result.getCourses();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.import_confirm_title)
                .setMessage(activity.getString(R.string.import_confirm_message,
                        courses.size(), result.getMethodHint()))
                .setPositiveButton("合并导入", (dialog, which) ->
                        repository.mergeCourses(courses, (added, skipped) -> {
                            AlarmScheduler.scheduleAllAlarms(activity);
                            String msg = "新增 " + added + " 条";
                            if (skipped > 0) {
                                msg += "，跳过 " + skipped + " 条重复";
                            }
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        }))
                .setNeutralButton("清空后导入", (dialog, which) -> {
                    repository.replaceAll(courses, () -> {
                        AlarmScheduler.scheduleAllAlarms(activity);
                        Toast.makeText(activity,
                                activity.getString(R.string.import_success, courses.size()),
                                Toast.LENGTH_SHORT).show();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                })
                .show();
    }
}
