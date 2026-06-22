package com.schedule.app.ui.settings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.schedule.app.R;
import com.schedule.app.notification.AlarmScheduler;
import com.schedule.app.util.SectionTimeMapper;

import java.util.Locale;

/**
 * 节次时间配置页：3 参数模板 + 预览列表 + 点击覆盖。
 * <p>
 * 用户只需设置第一节课开始时间、每节课时长、课间休息时长，
 * 所有节次自动计算。可点击预览列表中的任意节次单独覆盖开始时间，
 * 后续节次自动级联推算。
 */
public class SectionTimeConfigActivity extends AppCompatActivity {

    private TextView tvFirstStart;
    private Slider sliderDuration, sliderBreak;
    private TextView tvDurationValue, tvBreakValue;
    private LinearLayout previewContainer;
    private MaterialButton btnReset;

    /** 每节预览行的引用，方便刷新 */
    private View[] previewRows;
    private TextView[] tvSections, tvTimes, tvTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_time_config);

        setupToolbar();
        bindViews();
        loadCurrentValues();
        setupListeners();
        refreshPreview();
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
        tvFirstStart = findViewById(R.id.tvFirstStart);
        sliderDuration = findViewById(R.id.sliderDuration);
        sliderBreak = findViewById(R.id.sliderBreak);
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvBreakValue = findViewById(R.id.tvBreakValue);
        previewContainer = findViewById(R.id.previewContainer);
        btnReset = findViewById(R.id.btnReset);
    }

    private void loadCurrentValues() {
        tvFirstStart.setText(SectionTimeMapper.getFirstStartTime(this));
        sliderDuration.setValue(SectionTimeMapper.getDuration(this));
        sliderBreak.setValue(SectionTimeMapper.getBreakMinutes(this));
        updateLabels();
    }

    private void setupListeners() {
        // 第一节课开始时间
        findViewById(R.id.rowFirstStart).setOnClickListener(v -> showTimePicker(
                "第一节课开始时间",
                tvFirstStart.getText().toString(),
                (h, m) -> {
                    String time = String.format(Locale.CHINA, "%02d:%02d", h, m);
                    tvFirstStart.setText(time);
                    saveTemplate();
                    refreshPreview();
                }));

        // 课时长滑块
        sliderDuration.addOnChangeListener((slider, value, fromUser) -> {
            tvDurationValue.setText((int) value + " 分钟");
            if (fromUser) {
                saveTemplate();
                refreshPreview();
            }
        });

        // 课间休息滑块
        sliderBreak.addOnChangeListener((slider, value, fromUser) -> {
            tvBreakValue.setText((int) value + " 分钟");
            if (fromUser) {
                saveTemplate();
                refreshPreview();
            }
        });

        // 恢复默认
        btnReset.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("恢复默认值")
                    .setMessage("将清除所有自定义设置，恢复为默认节次时间。")
                    .setPositiveButton("恢复", (dialog, which) -> {
                        SectionTimeMapper.resetAll(this);
                        loadCurrentValues();
                        refreshPreview();
                        AlarmScheduler.scheduleAllAlarms(this);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void saveTemplate() {
        String firstStart = tvFirstStart.getText().toString();
        int duration = (int) sliderDuration.getValue();
        int breakMin = (int) sliderBreak.getValue();
        SectionTimeMapper.saveTemplate(this, firstStart, duration, breakMin);
        AlarmScheduler.scheduleAllAlarms(this);
    }

    private void updateLabels() {
        tvDurationValue.setText((int) sliderDuration.getValue() + " 分钟");
        tvBreakValue.setText((int) sliderBreak.getValue() + " 分钟");
    }

    /**
     * 重建预览列表：为每个节次创建一行，显示节次编号 + 时间 + 自定义标签。
     */
    private void refreshPreview() {
        int total = SectionTimeMapper.getTotalSections(this);
        String[][] times = SectionTimeMapper.calcAllSections(this);

        previewContainer.removeAllViews();
        previewRows = new View[total];
        tvSections = new TextView[total];
        tvTimes = new TextView[total];
        tvTags = new TextView[total];

        for (int i = 0; i < total; i++) {
            final int section = i + 1;
            boolean overridden = SectionTimeMapper.isOverridden(this, section);

            // 行容器
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padH = dp(12);
            int padV = dp(10);
            row.setPadding(padH, padV, padH, padV);
            row.setClickable(true);
            row.setFocusable(true);
            // 可点击反馈
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            row.setBackgroundResource(outValue.resourceId);

            // 节次编号
            TextView tvSec = new TextView(this);
            tvSec.setText("第" + section + "节");
            tvSec.setTextColor(getColor(R.color.on_background));
            tvSec.setTextSize(15);
            tvSec.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            LinearLayout.LayoutParams lpSec = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0);
            tvSec.setLayoutParams(lpSec);

            // 时间
            TextView tvTime = new TextView(this);
            tvTime.setText(times[i][0] + " - " + times[i][1]);
            tvTime.setTextColor(overridden ? getColor(R.color.primary) : getColor(R.color.text_secondary));
            tvTime.setTextSize(14);
            tvTime.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lpTime = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            tvTime.setLayoutParams(lpTime);

            // 自定义标签
            TextView tvTag = new TextView(this);
            tvTag.setText(overridden ? "自定义" : "");
            tvTag.setTextColor(getColor(R.color.primary));
            tvTag.setTextSize(11);
            tvTag.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams lpTag = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvTag.setLayoutParams(lpTag);

            row.addView(tvSec);
            row.addView(tvTime);
            row.addView(tvTag);

            // 点击：弹出 TimePicker 覆盖该节开始时间
            row.setOnClickListener(v -> {
                String currentTime = times[section - 1][0];
                showTimePicker("第" + section + "节 开始时间", currentTime, (h, m) -> {
                    String time = String.format(Locale.CHINA, "%02d:%02d", h, m);
                    SectionTimeMapper.saveSectionOverride(this, section, time);
                    refreshPreview();
                });
            });

            // 长按：清除覆盖
            row.setOnLongClickListener(v -> {
                if (SectionTimeMapper.isOverridden(this, section)) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("清除自定义时间")
                            .setMessage("第" + section + "节将恢复为模板自动计算的时间。")
                            .setPositiveButton("清除", (dialog, which) -> {
                                SectionTimeMapper.clearSectionOverride(this, section);
                                refreshPreview();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
                return true;
            });

            // 分隔线（最后一行不加）
            if (i < total - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(getColor(R.color.divider_soft));
                LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                lpDiv.leftMargin = dp(12);
                divider.setLayoutParams(lpDiv);

                LinearLayout wrapper = new LinearLayout(this);
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.addView(row);
                wrapper.addView(divider);
                previewContainer.addView(wrapper);
                previewRows[i] = wrapper;
            } else {
                previewContainer.addView(row);
                previewRows[i] = row;
            }

            tvSections[i] = tvSec;
            tvTimes[i] = tvTime;
            tvTags[i] = tvTag;
        }
    }

    /**
     * 弹出 Material TimePicker。
     */
    private void showTimePicker(String title, String currentTime, OnTimeSetCallback callback) {
        int hour = 8, minute = 0;
        try {
            String[] parts = currentTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(title)
                .build();

        picker.addOnPositiveButtonClickListener(v ->
                callback.onTimeSet(picker.getHour(), picker.getMinute()));

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private interface OnTimeSetCallback {
        void onTimeSet(int hour, int minute);
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
