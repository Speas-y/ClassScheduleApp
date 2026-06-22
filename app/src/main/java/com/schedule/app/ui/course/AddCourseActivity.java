package com.schedule.app.ui.course;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.schedule.app.R;
import com.schedule.app.data.entity.Course;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.util.SectionTimeMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 手动添加或编辑课程（含节次、周次、单双周、颜色），与导入解析写入同一 {@link Course} 表结构。
 */
public class AddCourseActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private static final int[] PRESET_COLORS = {
            0xFF2F7DFF, 0xFF71D99E, 0xFFA88AF7, 0xFFFFB25E,
            0xFFFF8BA7, 0xFFFFD36B, 0xFF8ED8F8, 0xFFB7C4D8,
            0xFF4FC3F7, 0xFF81C784, 0xFFFFA726, 0xFFF06292
    };

    private TextInputEditText etCourseName, etLocation, etTeacher;
    private ChipGroup chipGroupDay, chipGroupWeekType;
    private Spinner spinnerStartSection, spinnerEndSection, spinnerStartWeek, spinnerEndWeek;
    private GridLayout gridColors;
    private MaterialButton btnSave;

    private CourseRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int selectedColor = PRESET_COLORS[0];
    private int editingCourseId = -1;
    private View lastSelectedColorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        repository = CourseRepository.getInstance(getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupSpinners();
        setupColorPicker();

        editingCourseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        if (editingCourseId != -1) {
            toolbar.setTitle("编辑课程");
            loadCourse(editingCourseId);
        } else {
            toolbar.setTitle("添加课程");
        }

        btnSave.setOnClickListener(v -> saveCourse());
    }

    private void bindViews() {
        etCourseName = findViewById(R.id.etCourseName);
        etLocation = findViewById(R.id.etLocation);
        etTeacher = findViewById(R.id.etTeacher);
        chipGroupDay = findViewById(R.id.chipGroupDay);
        chipGroupWeekType = findViewById(R.id.chipGroupWeekType);
        spinnerStartSection = findViewById(R.id.spinnerStartSection);
        spinnerEndSection = findViewById(R.id.spinnerEndSection);
        spinnerStartWeek = findViewById(R.id.spinnerStartWeek);
        spinnerEndWeek = findViewById(R.id.spinnerEndWeek);
        gridColors = findViewById(R.id.gridColors);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupSpinners() {
        int totalSections = SectionTimeMapper.getTotalSections(this);

        List<String> sectionItems = new ArrayList<>();
        for (int i = 1; i <= totalSections; i++) {
            sectionItems.add("第 " + i + " 节");
        }
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sectionItems);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartSection.setAdapter(sectionAdapter);
        spinnerEndSection.setAdapter(sectionAdapter);
        spinnerEndSection.setSelection(1);

        List<String> weekItems = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            weekItems.add("第 " + i + " 周");
        }
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, weekItems);
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartWeek.setAdapter(weekAdapter);
        spinnerEndWeek.setAdapter(weekAdapter);
        spinnerEndWeek.setSelection(15);
    }

    private void setupColorPicker() {
        int size = dp(36);
        int margin = dp(8);

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int color = PRESET_COLORS[i];

            // 颜色本身用实心圆展示，选中态用外圈描边，和卡片式表单保持轻量视觉。
            ImageView iv = new ImageView(this);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            circle.setSize(size, size);
            iv.setImageDrawable(circle);
            iv.setPadding(dp(4), dp(4), dp(4), dp(4));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size + margin;
            params.height = size + margin;
            params.setGravity(Gravity.CENTER);
            iv.setLayoutParams(params);

            applyColorSelection(iv, color == selectedColor);
            if (color == selectedColor) lastSelectedColorView = iv;

            iv.setOnClickListener(v -> {
                if (lastSelectedColorView != null) {
                    applyColorSelection(lastSelectedColorView, false);
                }
                selectedColor = color;
                applyColorSelection(v, true);
                lastSelectedColorView = v;
            });

            gridColors.addView(iv);
        }
    }

    private void loadCourse(int courseId) {
        // Room 查询放到后台线程，避免编辑课程时阻塞界面首帧。
        executor.execute(() -> {
            Course course = repository.getCourseById(courseId);
            if (course == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "课程不存在", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }
            runOnUiThread(() -> populateForm(course));
        });
    }

    private void populateForm(Course course) {
        etCourseName.setText(course.getCourseName());
        etLocation.setText(course.getLocation());
        etTeacher.setText(course.getTeacher());

        int dayChipId = getDayChipId(course.getDayOfWeek());
        if (dayChipId != -1) {
            Chip chip = findViewById(dayChipId);
            if (chip != null) chip.setChecked(true);
        }

        spinnerStartSection.setSelection(course.getStartSection() - 1);
        spinnerEndSection.setSelection(course.getEndSection() - 1);
        spinnerStartWeek.setSelection(course.getStartWeek() - 1);
        spinnerEndWeek.setSelection(course.getEndWeek() - 1);

        int weekTypeChipId;
        switch (course.getWeekType()) {
            case 1: weekTypeChipId = R.id.chipOddWeek; break;
            case 2: weekTypeChipId = R.id.chipEvenWeek; break;
            default: weekTypeChipId = R.id.chipEveryWeek; break;
        }
        ((Chip) findViewById(weekTypeChipId)).setChecked(true);

        selectedColor = course.getColor();
        updateColorSelection();
    }

    private void updateColorSelection() {
        for (int i = 0; i < gridColors.getChildCount(); i++) {
            View child = gridColors.getChildAt(i);
            boolean selected = i < PRESET_COLORS.length && PRESET_COLORS[i] == selectedColor;
            applyColorSelection(child, selected);
            if (selected) {
                lastSelectedColorView = child;
            }
        }
    }

    private void applyColorSelection(View view, boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.TRANSPARENT);
        // 使用当前选中的课程色作为描边，让用户能直观看到保存后的色块颜色。
        background.setStroke(selected ? dp(3) : 0, selectedColor);
        view.setBackground(background);
    }

    private int getDayChipId(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return R.id.chipMon;
            case 2: return R.id.chipTue;
            case 3: return R.id.chipWed;
            case 4: return R.id.chipThu;
            case 5: return R.id.chipFri;
            case 6: return R.id.chipSat;
            case 7: return R.id.chipSun;
            default: return -1;
        }
    }

    private int getDayOfWeek() {
        int checkedId = chipGroupDay.getCheckedChipId();
        if (checkedId == R.id.chipMon) return 1;
        if (checkedId == R.id.chipTue) return 2;
        if (checkedId == R.id.chipWed) return 3;
        if (checkedId == R.id.chipThu) return 4;
        if (checkedId == R.id.chipFri) return 5;
        if (checkedId == R.id.chipSat) return 6;
        if (checkedId == R.id.chipSun) return 7;
        return -1;
    }

    private int getWeekType() {
        int checkedId = chipGroupWeekType.getCheckedChipId();
        if (checkedId == R.id.chipOddWeek) return 1;
        if (checkedId == R.id.chipEvenWeek) return 2;
        return 0;
    }

    private void saveCourse() {
        String name = getText(etCourseName);
        String location = getText(etLocation);
        String teacher = getText(etTeacher);

        if (name.isEmpty()) {
            etCourseName.setError("请输入课程名称");
            etCourseName.requestFocus();
            return;
        }

        int dayOfWeek = getDayOfWeek();
        if (dayOfWeek == -1) {
            Toast.makeText(this, "请选择星期", Toast.LENGTH_SHORT).show();
            return;
        }

        int startSection = spinnerStartSection.getSelectedItemPosition() + 1;
        int endSection = spinnerEndSection.getSelectedItemPosition() + 1;
        if (endSection < startSection) {
            Toast.makeText(this, "结束节次不能小于开始节次", Toast.LENGTH_SHORT).show();
            return;
        }

        int startWeek = spinnerStartWeek.getSelectedItemPosition() + 1;
        int endWeek = spinnerEndWeek.getSelectedItemPosition() + 1;
        if (endWeek < startWeek) {
            Toast.makeText(this, "结束周不能小于开始周", Toast.LENGTH_SHORT).show();
            return;
        }

        int weekType = getWeekType();
        final int finalDayOfWeek = dayOfWeek;
        final int finalStartSection = startSection;
        final int finalEndSection = endSection;
        final int finalStartWeek = startWeek;
        final int finalEndWeek = endWeek;
        final int finalWeekType = weekType;

        // 冲突检测：查同一天、节次有交集的课程
        executor.execute(() -> {
            List<Course> conflicting = repository
                    .findConflicting(finalDayOfWeek, finalStartSection, finalEndSection);

            // 过滤：排除自身（编辑模式）、检查周次重叠和单双周兼容
            List<Course> realConflicts = new java.util.ArrayList<>();
            for (Course c : conflicting) {
                if (c.getId() == editingCourseId) continue; // 排除自身
                if (!weeksOverlap(finalStartWeek, finalEndWeek, finalWeekType,
                        c.getStartWeek(), c.getEndWeek(), c.getWeekType())) continue;
                realConflicts.add(c);
            }

            runOnUiThread(() -> {
                if (realConflicts.isEmpty()) {
                    doSaveCourse(name, teacher, location, finalDayOfWeek,
                            finalStartSection, finalEndSection,
                            finalStartWeek, finalEndWeek, finalWeekType);
                } else {
                    showConflictDialog(realConflicts, name, teacher, location,
                            finalDayOfWeek, finalStartSection, finalEndSection,
                            finalStartWeek, finalEndWeek, finalWeekType);
                }
            });
        });
    }

    /**
     * 判断两门课的周次范围和单双周是否可能冲突。
     */
    private boolean weeksOverlap(int startW1, int endW1, int type1,
                                 int startW2, int endW2, int type2) {
        // 周次范围无交集则不冲突
        if (startW1 > endW2 || startW2 > endW1) return false;
        // 两门都是每周则一定冲突
        if (type1 == 0 && type2 == 0) return true;
        // 一门每周与另一门单/双周冲突
        if (type1 == 0 || type2 == 0) return true;
        // 两门都是单周或都是双周则冲突
        if (type1 == type2) return true;
        // 一门单周一门双周不冲突
        return false;
    }

    private void showConflictDialog(List<Course> conflicts,
                                    String name, String teacher, String location,
                                    int dayOfWeek, int startSec, int endSec,
                                    int startWeek, int endWeek, int weekType) {
        StringBuilder msg = new StringBuilder("以下课程与当前课程时间冲突：\n");
        for (Course c : conflicts) {
            String wt = c.getWeekType() == 1 ? "单周" : (c.getWeekType() == 2 ? "双周" : "每周");
            msg.append("• ").append(c.getCourseName())
                    .append("（第").append(c.getStartSection()).append("-").append(c.getEndSection())
                    .append("节，").append(wt).append("）\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("时间冲突")
                .setMessage(msg.toString())
                .setPositiveButton("仍然保存", (dialog, which) ->
                        doSaveCourse(name, teacher, location, dayOfWeek,
                                startSec, endSec, startWeek, endWeek, weekType))
                .setNegativeButton("取消", null)
                .show();
    }

    private void doSaveCourse(String name, String teacher, String location,
                              int dayOfWeek, int startSec, int endSec,
                              int startWeek, int endWeek, int weekType) {
        Course course = new Course(name, teacher, location, dayOfWeek,
                startSec, endSec, startWeek, endWeek, weekType, selectedColor);

        if (editingCourseId != -1) {
            course.setId(editingCourseId);
            repository.update(course);
            Toast.makeText(this, "课程已更新", Toast.LENGTH_SHORT).show();
        } else {
            repository.insert(course);
            Toast.makeText(this, "课程已添加", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
