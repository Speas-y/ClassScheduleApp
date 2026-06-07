package com.schedule.app.ui.schedule;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.schedule.app.R;
import com.schedule.app.data.entity.Course;
import com.schedule.app.ui.course.AddCourseActivity;
import com.schedule.app.util.ScheduleConstants;
import com.schedule.app.util.SectionTimeMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 课表主 UI：绑定 {@link ScheduleView} 与周切换，观察课程与周次变化后刷新网格。
 */
public class ScheduleFragment extends Fragment {

    private static final String[] DAY_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    private ScheduleViewModel viewModel;
    private ScheduleView scheduleView;
    private TextView tvWeek;
    private TextView[] dayViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ScheduleViewModel.class);

        scheduleView = view.findViewById(R.id.scheduleView);
        tvWeek = view.findViewById(R.id.tvCurrentWeek);
        dayViews = new TextView[]{
                view.findViewById(R.id.tvDayMon),
                view.findViewById(R.id.tvDayTue),
                view.findViewById(R.id.tvDayWed),
                view.findViewById(R.id.tvDayThu),
                view.findViewById(R.id.tvDayFri),
                view.findViewById(R.id.tvDaySat),
                view.findViewById(R.id.tvDaySun)
        };
        ImageButton btnPrev = view.findViewById(R.id.btnPrevWeek);
        ImageButton btnNext = view.findViewById(R.id.btnNextWeek);

        btnPrev.setOnClickListener(v -> viewModel.previousWeek());
        btnNext.setOnClickListener(v -> viewModel.nextWeek());

        tvWeek.setOnClickListener(v -> showWeekPicker());

        scheduleView.setOnCourseClickListener(this::showCourseDetail);

        viewModel.getCurrentWeek().observe(getViewLifecycleOwner(), week -> {
            tvWeek.setText("第 " + week + " 周");
            updateDayHeader(week);
            refreshSchedule();
        });

        viewModel.getAllCourses().observe(getViewLifecycleOwner(), courses ->
                refreshSchedule()
        );
    }

    private void updateDayHeader(int selectedWeek) {
        int currentWeek = viewModel.calculateCurrentWeek();
        LocalDate monday = LocalDate.now()
                .plusWeeks((long) selectedWeek - currentWeek)
                .with(DayOfWeek.MONDAY);
        int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;
        boolean highlightToday = selectedWeek == currentWeek;

        for (int i = 0; i < dayViews.length; i++) {
            TextView dayView = dayViews[i];
            dayView.setText(DAY_NAMES[i] + "\n" + monday.plusDays(i).format(DAY_FORMATTER));
            dayView.setTypeface(Typeface.DEFAULT, highlightToday && i == todayIndex
                    ? Typeface.BOLD : Typeface.NORMAL);
            dayView.setBackgroundResource(highlightToday && i == todayIndex
                    ? R.drawable.bg_day_selected : 0);
            dayView.setTextColor(ContextCompat.getColor(requireContext(), highlightToday && i == todayIndex
                    ? android.R.color.white
                    : (i >= 5 ? R.color.text_tertiary : R.color.text_secondary)));
        }
    }

    private void refreshSchedule() {
        List<Course> courses = viewModel.getAllCourses().getValue();
        Integer week = viewModel.getCurrentWeek().getValue();
        if (courses != null && week != null) {
            scheduleView.setCourses(courses, week);
        }
    }

    private void showWeekPicker() {
        int maxWeek = ScheduleConstants.MAX_TEACHING_WEEK;
        String[] weeks = new String[maxWeek];
        for (int i = 0; i < maxWeek; i++) weeks[i] = "第 " + (i + 1) + " 周";

        Integer current = viewModel.getCurrentWeek().getValue();
        int selected = current != null ? current - 1 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle("选择周次")
                .setSingleChoiceItems(weeks, selected, (dialog, which) -> {
                    viewModel.setWeek(which + 1);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCourseDetail(Course course) {
        String timeRange = SectionTimeMapper.getSectionRangeDisplay(
                requireContext(), course.getStartSection(), course.getEndSection());

        String weekTypeStr;
        switch (course.getWeekType()) {
            case 1: weekTypeStr = "(单周)"; break;
            case 2: weekTypeStr = "(双周)"; break;
            default: weekTypeStr = ""; break;
        }

        String msg = "教师：" + course.getTeacher() + "\n"
                + "地点：" + course.getLocation() + "\n"
                + "时间：" + timeRange + "\n"
                + "节次：第 " + course.getStartSection() + "-" + course.getEndSection() + " 节\n"
                + "周次：第 " + course.getStartWeek() + "-" + course.getEndWeek() + " 周 " + weekTypeStr;

        new AlertDialog.Builder(requireContext())
                .setTitle(course.getCourseName())
                .setMessage(msg)
                .setPositiveButton("确定", null)
                .setNeutralButton("编辑", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), AddCourseActivity.class);
                    intent.putExtra(AddCourseActivity.EXTRA_COURSE_ID, course.getId());
                    startActivity(intent);
                })
                .setNegativeButton("删除", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("确认删除")
                            .setMessage("确定要删除「" + course.getCourseName() + "」吗？")
                            .setPositiveButton("删除", (d2, w2) ->
                                    viewModel.getRepository().delete(course))
                            .setNegativeButton("取消", null)
                            .show();
                })
                .show();
    }
}
