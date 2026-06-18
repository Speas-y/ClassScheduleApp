package com.schedule.app.ui.settings;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.schedule.app.R;
import com.schedule.app.data.entity.Course;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.notification.AlarmScheduler;
import com.schedule.app.ui.import_.ImportActivity;
import com.schedule.app.ui.import_.KbcxMarkdownParser;
import com.schedule.app.util.SectionTimeMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
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
    private SwitchMaterial switchNotify;

    private final ActivityResultLauncher<String[]> markdownImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    this::onMarkdownDocumentPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

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
        switchNotify = findViewById(R.id.switchNotify);
    }

    private void setupClickListeners() {
        // 每一行设置项都对应一个原偏好项，保留既有 SharedPreferences 键名以兼容旧数据。
        findViewById(R.id.rowSemesterStart).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.rowTotalSections).setOnClickListener(v -> showTotalSectionsDialog());
        findViewById(R.id.rowSectionTimes).setOnClickListener(v -> showSectionTimesDialog());
        findViewById(R.id.rowImportMarkdown).setOnClickListener(v -> markdownImportLauncher.launch(new String[]{
                "text/markdown",
                "text/plain",
                "text/*",
                "*/*"
        }));
        findViewById(R.id.rowImportWeb)
                .setOnClickListener(v -> startActivity(new Intent(this, ImportActivity.class)));
        findViewById(R.id.rowClearCourses).setOnClickListener(v -> showClearCoursesDialog());
        setNotifySwitchListener();
    }

    private void setNotifySwitchListener() {
        // 开关只负责写入设置，真正的闹钟注册/取消统一在偏好监听里处理。
        switchNotify.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("notify_enabled", isChecked).apply());
    }

    private void onMarkdownDocumentPicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(this, "无法读取所选文件", Toast.LENGTH_SHORT).show();
                return;
            }
            String text = readStreamAsUtf8String(is);
            KbcxMarkdownParser parser = new KbcxMarkdownParser();
            List<Course> courses = parser.parse(text);
            if (courses.isEmpty()) {
                Toast.makeText(this,
                        "未解析到课程。请确认文件是由 format_kbcx 生成的 kbcx_schedule.md，且包含「按星期 · 节次」表格。",
                        Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("导入课表")
                    .setMessage("解析到 " + courses.size()
                            + " 条课表记录（不同周次会拆成多条）。将删除本地已有课程并导入，是否继续？")
                    .setPositiveButton("导入", (dialog, which) -> {
                        // 导入采用全量替换，避免旧课表与新课表混在同一个教学周期里。
                        CourseRepository repo = CourseRepository.getInstance(getApplication());
                        repo.deleteAll();
                        repo.insertAllAndCallback(courses, () -> runOnUiThread(() -> {
                            AlarmScheduler.scheduleAllAlarms(this);
                            Toast.makeText(this, "已导入 " + courses.size() + " 条记录",
                                    Toast.LENGTH_SHORT).show();
                        }));
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (IOException e) {
            Toast.makeText(this, "读取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String readStreamAsUtf8String(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = is.read(b)) != -1) {
            buf.write(b, 0, n);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    private void showDatePicker() {
        String saved = prefs.getString("semester_start_date", "");

        Calendar cal = Calendar.getInstance();
        if (!saved.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(saved);
                cal.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
            } catch (Exception ignored) {
                // Invalid legacy value should not block choosing a new date.
            }
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.CHINA, "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            prefs.edit().putString("semester_start_date", dateStr).apply();
            AlarmScheduler.scheduleAllAlarms(this);
            Toast.makeText(this, "学期开始日期已设置为 " + dateStr, Toast.LENGTH_SHORT).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTotalSectionsDialog() {
        String[] entries = getResources().getStringArray(R.array.section_count_entries);
        String[] values = getResources().getStringArray(R.array.section_count_values);
        String current = String.valueOf(getTotalSections());
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("每日节数")
                .setSingleChoiceItems(entries, selected, (dialog, which) -> {
                    prefs.edit().putString("total_sections", values[which]).apply();
                    AlarmScheduler.scheduleAllAlarms(this);
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
                    Toast.makeText(this, "已清空所有课程", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateSummaries() {
        String date = prefs.getString("semester_start_date", "");
        tvSemesterSummary.setText(date.isEmpty() ? "未设置，点击选择日期" : date);
        tvTotalSectionsSummary.setText("当前: " + getTotalSections() + " 节");
        tvSectionTimesSummary.setText(buildSectionTimesSummary());

        // 程序刷新开关状态时临时移除监听，避免触发一次无意义的偏好写入。
        switchNotify.setOnCheckedChangeListener(null);
        switchNotify.setChecked(prefs.getBoolean("notify_enabled", true));
        setNotifySwitchListener();
    }

    private int getTotalSections() {
        return SectionTimeMapper.getTotalSections(this);
    }

    private String buildSectionTimesSummary() {
        int total = getTotalSections();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= total; i += 2) {
            int end = Math.min(i + 1, total);
            String startTime = SectionTimeMapper.getStartTime(this, i);
            String endTime = SectionTimeMapper.getEndTime(this, end);
            if (i == end) {
                sb.append("第").append(i).append("节: ").append(startTime).append("-").append(endTime);
            } else {
                sb.append("第").append(i).append("-").append(end)
                        .append("节: ").append(startTime).append("-").append(endTime);
            }
            if (i + 1 < total) sb.append("\n");
        }
        return sb.toString();
    }

    private String[] buildSectionItems() {
        int total = getTotalSections();
        String[] items = new String[total];
        for (int i = 0; i < total; i++) {
            int section = i + 1;
            String start = SectionTimeMapper.getStartTime(this, section);
            String end = SectionTimeMapper.getEndTime(this, section);
            items[i] = "第" + section + "节:  " + start + " - " + end;
        }
        return items;
    }

    private void showSectionTimesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("节次时间配置")
                .setItems(buildSectionItems(), (dialog, which) -> showStartTimePicker(which + 1))
                .setNeutralButton("恢复默认", (dialog, which) -> resetAllSectionTimes())
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showStartTimePicker(int section) {
        String currentStart = SectionTimeMapper.getStartTime(this, section);
        int hour = Integer.parseInt(currentStart.split(":")[0]);
        int minute = Integer.parseInt(currentStart.split(":")[1]);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            String startTime = String.format(Locale.CHINA, "%02d:%02d", h, m);
            showEndTimePicker(section, startTime);
        }, hour, minute, true);
        dialog.setTitle("第" + section + "节 - 上课时间");
        dialog.show();
    }

    private void showEndTimePicker(int section, String startTime) {
        String currentEnd = SectionTimeMapper.getEndTime(this, section);
        int hour = Integer.parseInt(currentEnd.split(":")[0]);
        int minute = Integer.parseInt(currentEnd.split(":")[1]);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            String endTime = String.format(Locale.CHINA, "%02d:%02d", h, m);
            saveSectionTime(section, startTime, endTime);
        }, hour, minute, true);
        dialog.setTitle("第" + section + "节 - 下课时间");
        dialog.show();
    }

    private void saveSectionTime(int section, String startTime, String endTime) {
        prefs.edit()
                .putString("section_" + section + "_start", startTime)
                .putString("section_" + section + "_end", endTime)
                .apply();

        AlarmScheduler.scheduleAllAlarms(this);
        Toast.makeText(this, "第" + section + "节时间已更新: " + startTime + " - " + endTime,
                Toast.LENGTH_SHORT).show();
    }

    private void resetAllSectionTimes() {
        SharedPreferences.Editor editor = prefs.edit();
        // 只清除自定义节次时间，保留每日节数等其他设置。
        for (int i = 1; i <= SectionTimeMapper.MAX_SECTIONS; i++) {
            editor.remove("section_" + i + "_start");
            editor.remove("section_" + i + "_end");
        }
        editor.apply();

        AlarmScheduler.scheduleAllAlarms(this);
        Toast.makeText(this, "已恢复默认时间", Toast.LENGTH_SHORT).show();
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

        if (key.equals("notify_enabled")) {
            boolean enabled = sharedPreferences.getBoolean(key, true);
            if (enabled) {
                AlarmScheduler.scheduleAllAlarms(this);
            } else {
                AlarmScheduler.cancelAllAlarms(this);
            }
        }
    }
}
