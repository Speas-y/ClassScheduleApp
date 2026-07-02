package com.schedule.app.ui.settings;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.schedule.app.R;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.notification.AlarmScheduler;
import com.schedule.app.ui.import_.ImportConfirmDialog;
import com.schedule.app.ui.import_.ImportActivity;
import com.schedule.app.ui.import_.ImportResult;
import com.schedule.app.ui.import_.ScheduleImportService;
import com.schedule.app.util.SectionTimeMapper;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;

/**
 * 设置页：使用与课表页一致的卡片式 UI，维护学期首日、提醒、节次时间与导入相关设置。
 */
public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences prefs;
    private TextView tvSemesterSummary;
    private TextView tvTotalSectionsSummary;
    private TextView tvSectionTimesSummary;
    private SwitchMaterial switchBeforeClassNotify;
    private SwitchMaterial switchAfterClassNotify;
    private TextView tvBeforeClassTime;
    private TextView tvBeforeClassSummary;
    private TextView tvAfterClassSummary;
    private TextView tvReminderPermissionSummary;
    private CourseRepository repository;
    private ScheduleImportService importService;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                updateReminderPermissionSummary();
                if (granted) {
                    AlarmScheduler.scheduleAllAlarms(this);
                }
            });

    private final ActivityResultLauncher<String[]> markdownImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    this::onMarkdownDocumentPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        repository = CourseRepository.getInstance(getApplication());
        importService = new ScheduleImportService(this);

        setupToolbar();
        bindViews();
        setupClickListeners();
        updateSummaries();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvSemesterSummary = findViewById(R.id.tvSemesterSummary);
        tvTotalSectionsSummary = findViewById(R.id.tvTotalSectionsSummary);
        tvSectionTimesSummary = findViewById(R.id.tvSectionTimesSummary);
        switchBeforeClassNotify = findViewById(R.id.switchBeforeClassNotify);
        switchAfterClassNotify = findViewById(R.id.switchAfterClassNotify);
        tvBeforeClassTime = findViewById(R.id.tvBeforeClassTime);
        tvBeforeClassSummary = findViewById(R.id.tvBeforeClassSummary);
        tvAfterClassSummary = findViewById(R.id.tvAfterClassSummary);
        tvReminderPermissionSummary = findViewById(R.id.tvReminderPermissionSummary);
    }

    private void setupClickListeners() {
        findViewById(R.id.rowSemesterStart).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.rowTotalSections).setOnClickListener(v -> showTotalSectionsDialog());
        // 节次时间配置：跳转到新的独立页面
        findViewById(R.id.rowSectionTimes).setOnClickListener(v ->
                startActivity(new Intent(this, SectionTimeConfigActivity.class)));
        findViewById(R.id.rowImportMarkdown).setOnClickListener(v -> markdownImportLauncher.launch(new String[]{
                "text/markdown",
                "text/plain",
                "text/*",
                "*/*"
        }));
        findViewById(R.id.rowImportWeb)
                .setOnClickListener(v -> startActivity(new Intent(this, ImportActivity.class)));
        findViewById(R.id.rowClearCourses).setOnClickListener(v -> showClearCoursesDialog());
        findViewById(R.id.rowReminderPermission).setOnClickListener(v -> handleReminderPermissionClick());
        setNotifySwitchListener();
        setAfterClassNotifySwitchListener();
        setBeforeClassTimeClickListener();
    }

    private void setNotifySwitchListener() {
        switchBeforeClassNotify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notify_enabled", isChecked).apply();
        });
    }

    private void setAfterClassNotifySwitchListener() {
        switchAfterClassNotify.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("after_class_notify_enabled", isChecked).apply());
    }

    private void setBeforeClassTimeClickListener() {
        findViewById(R.id.rowBeforeClassTime).setOnClickListener(v -> showBeforeClassTimePicker());
    }

    private void onMarkdownDocumentPicked(@Nullable Uri uri) {
        if (uri == null) return;
        try {
            ImportResult result = importService.parseText(importService.readTextFromUri(uri));
            if (result.getCourses().isEmpty()) {
                Toast.makeText(this, "未解析到课程", Toast.LENGTH_LONG).show();
                return;
            }
            ImportConfirmDialog.show(this, repository, result, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showDatePicker() {
        String saved = prefs.getString("semester_start_date", "");
        Calendar cal = Calendar.getInstance();
        if (!saved.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(saved);
                cal.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
            } catch (Exception ignored) {
            }
        }

        // 使用原生的DatePickerDialog，更稳定
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            prefs.edit().putString("semester_start_date", dateStr).apply();
            Toast.makeText(this, "学期开始日期已设置为 " + dateStr, Toast.LENGTH_SHORT).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTotalSectionsDialog() {
        String[] entries = getResources().getStringArray(R.array.section_count_entries);
        String[] values = getResources().getStringArray(R.array.section_count_values);
        String current = String.valueOf(getTotalSections());
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) { selected = i; break; }
        }
        new AlertDialog.Builder(this)
                .setTitle("每日节数")
                .setSingleChoiceItems(entries, selected, (dialog, which) -> {
                    prefs.edit().putString("total_sections", values[which]).apply();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearCoursesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定要删除所有课程吗？此操作不可撤销。")
                .setPositiveButton("清空", (dialog, which) -> {
                    CourseRepository.getInstance(getApplication()).deleteAll();
                    AlarmScheduler.cancelAllAlarms(this);
                    Toast.makeText(this, "已清空所有课程", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateSummaries() {
        String date = prefs.getString("semester_start_date", "");
        tvSemesterSummary.setText(date.isEmpty() ? "未设置，点击选择日期" : date);
        tvTotalSectionsSummary.setText("当前: " + getTotalSections() + " 节");

        // 模板摘要
        String firstStart = SectionTimeMapper.getFirstStartTime(this);
        int duration = SectionTimeMapper.getDuration(this);
        int breakMin = SectionTimeMapper.getBreakMinutes(this);
        tvSectionTimesSummary.setText(firstStart + " 开始 · " + duration + "分钟/节 · 休息" + breakMin + "分钟");

        switchBeforeClassNotify.setOnCheckedChangeListener(null);
        switchBeforeClassNotify.setChecked(prefs.getBoolean("notify_enabled", true));
        setNotifySwitchListener();
        
        switchAfterClassNotify.setOnCheckedChangeListener(null);
        switchAfterClassNotify.setChecked(prefs.getBoolean("after_class_notify_enabled", false));
        setAfterClassNotifySwitchListener();
        setBeforeClassTimeClickListener();
        
        int beforeMinutes = prefs.getInt("before_class_reminder_minutes", 10);
        tvBeforeClassTime.setText(beforeMinutes + " 分钟");
        
        updateReminderSummary();
        updateReminderPermissionSummary();
    }

    private int getTotalSections() {
        return SectionTimeMapper.getTotalSections(this);
    }

    private void showBeforeClassTimePicker() {
        String[] options = {"5 分钟", "10 分钟", "15 分钟", "20 分钟", "30 分钟"};
        int[] values = {5, 10, 15, 20, 30};
        int currentValue = prefs.getInt("before_class_reminder_minutes", 10);
        int selectedIndex = 1; // 默认10分钟
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("提前提醒时间")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    prefs.edit().putInt("before_class_reminder_minutes", values[which]).apply();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateReminderSummary() {
        boolean enabled = prefs.getBoolean("notify_enabled", true);
        int minutes = prefs.getInt("before_class_reminder_minutes", 10);
        tvBeforeClassSummary.setText(enabled ? "上课前 " + minutes + " 分钟推送通知" : "已关闭");
        
        boolean afterClassEnabled = prefs.getBoolean("after_class_notify_enabled", false);
        tvAfterClassSummary.setText(afterClassEnabled ? "下课时推送通知" : "已关闭");
    }

    private void updateReminderPermissionSummary() {
        boolean notificationGranted = isNotificationPermissionGranted();
        boolean exactAlarmGranted = AlarmScheduler.canScheduleExactAlarms(this);

        if (notificationGranted && exactAlarmGranted) {
            tvReminderPermissionSummary.setText("通知与精确闹钟权限正常");
        } else if (!notificationGranted && !exactAlarmGranted) {
            tvReminderPermissionSummary.setText("通知和精确闹钟权限未开启，点击处理");
        } else if (!notificationGranted) {
            tvReminderPermissionSummary.setText("通知权限未开启，点击授权");
        } else {
            tvReminderPermissionSummary.setText("精确闹钟权限未开启，点击前往系统设置");
        }
    }

    private void handleReminderPermissionClick() {
        if (!isNotificationPermissionGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            return;
        }

        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:" + getPackageName())));
                }
            }
            return;
        }

        Toast.makeText(this, "提醒权限已开启", Toast.LENGTH_SHORT).show();
    }

    private boolean isNotificationPermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
        updateSummaries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        updateSummaries();
        if (isReminderScheduleKey(key)) {
            rescheduleCourseReminders();
        }
    }

    private boolean isReminderScheduleKey(String key) {
        if (key.equals("notify_enabled")
                || key.equals("after_class_notify_enabled")
                || key.equals("before_class_reminder_minutes")
                || key.equals("semester_start_date")
                || key.equals("total_sections")
                || key.equals(SectionTimeMapper.KEY_FIRST_START)
                || key.equals(SectionTimeMapper.KEY_DURATION)
                || key.equals(SectionTimeMapper.KEY_BREAK)) {
            return true;
        }
        return key.startsWith("section_") && (key.endsWith("_start") || key.endsWith("_end"));
    }

    private void rescheduleCourseReminders() {
        boolean beforeClassEnabled = prefs.getBoolean("notify_enabled", true);
        boolean afterClassEnabled = prefs.getBoolean("after_class_notify_enabled", false);
        if (beforeClassEnabled || afterClassEnabled) {
            AlarmScheduler.scheduleAllAlarms(this);
        } else {
            AlarmScheduler.cancelAllAlarms(this);
        }
    }
}
