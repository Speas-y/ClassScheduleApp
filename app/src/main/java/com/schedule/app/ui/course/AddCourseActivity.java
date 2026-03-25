package com.schedule.app.ui.course;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

import android.view.View;
import android.widget.ImageView;

public class AddCourseActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private static final int[] PRESET_COLORS = {
            0xFF4A90D9, 0xFF67C23A, 0xFFE6A23C, 0xFFF56C6C,
            0xFF909399, 0xFF9B59B6, 0xFF1ABC9C, 0xFFE74C3C,
            0xFF3498DB, 0xFF2ECC71, 0xFFF39C12, 0xFFE91E63
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

        repository = new CourseRepository(getApplication());

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

            if (color == selectedColor) {
                iv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                lastSelectedColorView = iv;
            }

            iv.setOnClickListener(v -> {
                if (lastSelectedColorView != null) {
                    lastSelectedColorView.setBackground(null);
                }
                v.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                lastSelectedColorView = v;
                selectedColor = color;
            });

            gridColors.addView(iv);
        }
    }

    private void loadCourse(int courseId) {
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
            child.setBackground(null);
            if (i < PRESET_COLORS.length && PRESET_COLORS[i] == selectedColor) {
                child.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                lastSelectedColorView = child;
            }
        }
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

        Course course = new Course(name, teacher, location, dayOfWeek,
                startSection, endSection, startWeek, endWeek, weekType, selectedColor);

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
