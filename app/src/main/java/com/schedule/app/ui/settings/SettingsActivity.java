package com.schedule.app.ui.settings;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.appbar.MaterialToolbar;
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

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private ActivityResultLauncher<String[]> markdownImportLauncher;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            markdownImportLauncher = registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::onMarkdownDocumentPicked);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            updateSummaries();

            Preference importMdPref = findPreference("import_kbcx_markdown");
            if (importMdPref != null) {
                importMdPref.setOnPreferenceClickListener(pref -> {
                    markdownImportLauncher.launch(new String[]{
                            "text/markdown",
                            "text/plain",
                            "text/*",
                            "*/*"
                    });
                    return true;
                });
            }

            Preference importWebPref = findPreference("import_jwxt_web");
            if (importWebPref != null) {
                importWebPref.setOnPreferenceClickListener(pref -> {
                    startActivity(new Intent(requireContext(), ImportActivity.class));
                    return true;
                });
            }

            Preference semesterDatePref = findPreference("semester_start_date");
            if (semesterDatePref != null) {
                semesterDatePref.setOnPreferenceClickListener(pref -> {
                    showDatePicker();
                    return true;
                });
            }

            Preference sectionTimesPref = findPreference("section_times_config");
            if (sectionTimesPref != null) {
                sectionTimesPref.setSummary(buildSectionTimesSummary());
                sectionTimesPref.setOnPreferenceClickListener(pref -> {
                    showSectionTimesDialog();
                    return true;
                });
            }

            Preference clearPref = findPreference("clear_all_courses");
            if (clearPref != null) {
                clearPref.setOnPreferenceClickListener(pref -> {
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("确认清空")
                            .setMessage("确定要删除所有课程吗？此操作不可撤销。")
                            .setPositiveButton("清空", (dialog, which) -> {
                                new CourseRepository(requireActivity().getApplication()).deleteAll();
                                Toast.makeText(requireContext(), "已清空所有课程", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    return true;
                });
            }
        }

        private void onMarkdownDocumentPicked(@Nullable Uri uri) {
            if (uri == null) {
                return;
            }
            try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                if (is == null) {
                    Toast.makeText(requireContext(), "无法读取所选文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                String text = readStreamAsUtf8String(is);
                KbcxMarkdownParser parser = new KbcxMarkdownParser();
                List<Course> courses = parser.parse(text);
                if (courses.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "未解析到课程。请确认文件是由 format_kbcx 生成的 kbcx_schedule.md，且包含「按星期 · 节次」表格。",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("导入课表")
                        .setMessage("解析到 " + courses.size()
                                + " 条课表记录（不同周次会拆成多条）。将删除本地已有课程并导入，是否继续？")
                        .setPositiveButton("导入", (dialog, which) -> {
                            CourseRepository repo =
                                    new CourseRepository(requireActivity().getApplication());
                            repo.deleteAll();
                            repo.insertAllAndCallback(courses, () ->
                                    requireActivity().runOnUiThread(() -> {
                                        AlarmScheduler.scheduleAllAlarms(requireContext());
                                        Toast.makeText(requireContext(),
                                                "已导入 " + courses.size() + " 条记录",
                                                Toast.LENGTH_SHORT).show();
                                    }));
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } catch (IOException e) {
                Toast.makeText(requireContext(),
                        "读取失败: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
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
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String saved = prefs.getString("semester_start_date", "");

            Calendar cal = Calendar.getInstance();
            if (!saved.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(saved);
                    cal.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                } catch (Exception ignored) {}
            }

            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                String dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(requireContext()).edit();
                editor.putString("semester_start_date", dateStr).apply();
                updateSummaries();
                AlarmScheduler.scheduleAllAlarms(requireContext());
                Toast.makeText(requireContext(), "学期开始日期已设置为 " + dateStr, Toast.LENGTH_SHORT).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        }

        private void updateSummaries() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

            Preference semesterPref = findPreference("semester_start_date");
            if (semesterPref != null) {
                String date = prefs.getString("semester_start_date", "");
                semesterPref.setSummary(date.isEmpty() ? "未设置（点击选择日期）" : date);
            }

            ListPreference sectionsPref = findPreference("total_sections");
            if (sectionsPref != null) {
                sectionsPref.setSummary("当前: " + getTotalSections() + " 节");
            }

            Preference sectionTimesPref = findPreference("section_times_config");
            if (sectionTimesPref != null) {
                sectionTimesPref.setSummary(buildSectionTimesSummary());
            }
        }

        private int getTotalSections() {
            return SectionTimeMapper.getTotalSections(requireContext());
        }

        private String buildSectionTimesSummary() {
            int total = getTotalSections();
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= total; i += 2) {
                int end = Math.min(i + 1, total);
                String startTime = SectionTimeMapper.getStartTime(requireContext(), i);
                String endTime = SectionTimeMapper.getEndTime(requireContext(), end);
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
                String start = SectionTimeMapper.getStartTime(requireContext(), section);
                String end = SectionTimeMapper.getEndTime(requireContext(), section);
                items[i] = "第" + section + "节:  " + start + " - " + end;
            }
            return items;
        }

        private void showSectionTimesDialog() {
            new AlertDialog.Builder(requireContext())
                    .setTitle("节次时间配置")
                    .setItems(buildSectionItems(), (dialog, which) -> {
                        int section = which + 1;
                        showStartTimePicker(section);
                    })
                    .setNeutralButton("恢复默认", (dialog, which) -> {
                        resetAllSectionTimes();
                    })
                    .setNegativeButton("关闭", null)
                    .show();
        }

        private void showStartTimePicker(int section) {
            String currentStart = SectionTimeMapper.getStartTime(requireContext(), section);
            int hour = Integer.parseInt(currentStart.split(":")[0]);
            int minute = Integer.parseInt(currentStart.split(":")[1]);

            new TimePickerDialog(requireContext(), (view, h, m) -> {
                String startTime = String.format("%02d:%02d", h, m);
                showEndTimePicker(section, startTime);
            }, hour, minute, true)
            {{
                setTitle("第" + section + "节 - 上课时间");
            }}.show();
        }

        private void showEndTimePicker(int section, String startTime) {
            String currentEnd = SectionTimeMapper.getEndTime(requireContext(), section);
            int hour = Integer.parseInt(currentEnd.split(":")[0]);
            int minute = Integer.parseInt(currentEnd.split(":")[1]);

            new TimePickerDialog(requireContext(), (view, h, m) -> {
                String endTime = String.format("%02d:%02d", h, m);
                saveSectionTime(section, startTime, endTime);
            }, hour, minute, true)
            {{
                setTitle("第" + section + "节 - 下课时间");
            }}.show();
        }

        private void saveSectionTime(int section, String startTime, String endTime) {
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(requireContext()).edit();
            editor.putString("section_" + section + "_start", startTime);
            editor.putString("section_" + section + "_end", endTime);
            editor.apply();

            Preference pref = findPreference("section_times_config");
            if (pref != null) {
                pref.setSummary(buildSectionTimesSummary());
            }
            AlarmScheduler.scheduleAllAlarms(requireContext());
            Toast.makeText(requireContext(),
                    "第" + section + "节时间已更新: " + startTime + " - " + endTime,
                    Toast.LENGTH_SHORT).show();
        }

        private void resetAllSectionTimes() {
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(requireContext()).edit();
            for (int i = 1; i <= SectionTimeMapper.MAX_SECTIONS; i++) {
                editor.remove("section_" + i + "_start");
                editor.remove("section_" + i + "_end");
            }
            editor.apply();

            Preference pref = findPreference("section_times_config");
            if (pref != null) {
                pref.setSummary(buildSectionTimesSummary());
            }
            AlarmScheduler.scheduleAllAlarms(requireContext());
            Toast.makeText(requireContext(), "已恢复默认时间", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key == null) return;
            updateSummaries();

            if (key.equals("notify_enabled")) {
                boolean enabled = prefs.getBoolean(key, true);
                if (enabled) {
                    AlarmScheduler.scheduleAllAlarms(requireContext());
                }
            }
        }

        private void updateSectionTimesPreference() {
            Preference sectionTimesPref = findPreference("section_times_config");
            if (sectionTimesPref != null) {
                sectionTimesPref.setSummary(buildSectionTimesSummary());
            }
        }
    }
}
