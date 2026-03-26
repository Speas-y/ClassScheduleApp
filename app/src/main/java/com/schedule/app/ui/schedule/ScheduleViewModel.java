package com.schedule.app.ui.schedule;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.schedule.app.data.entity.Course;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.util.ScheduleConstants;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 课表页状态：课程列表来自 Repository；当前周次由设置中的学期首日推算，并限制在 1～{@link ScheduleConstants#MAX_TEACHING_WEEK} 周。
 */
public class ScheduleViewModel extends AndroidViewModel {

    private final CourseRepository repository;
    private final LiveData<List<Course>> allCourses;
    private final MutableLiveData<Integer> currentWeek = new MutableLiveData<>();

    public ScheduleViewModel(@NonNull Application application) {
        super(application);
        repository = CourseRepository.getInstance(application);
        allCourses = repository.getAllCourses();
        currentWeek.setValue(calculateCurrentWeek());
    }

    public LiveData<List<Course>> getAllCourses() {
        return allCourses;
    }

    public LiveData<Integer> getCurrentWeek() {
        return currentWeek;
    }

    public void setWeek(int week) {
        if (week >= 1 && week <= ScheduleConstants.MAX_TEACHING_WEEK) {
            currentWeek.setValue(week);
        }
    }

    public void previousWeek() {
        Integer w = currentWeek.getValue();
        if (w != null && w > 1) currentWeek.setValue(w - 1);
    }

    public void nextWeek() {
        Integer w = currentWeek.getValue();
        if (w != null && w < ScheduleConstants.MAX_TEACHING_WEEK) currentWeek.setValue(w + 1);
    }

    public int calculateCurrentWeek() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        String semesterStart = prefs.getString("semester_start_date", "");
        if (semesterStart.isEmpty()) return 1;

        try {
            LocalDate startDate = LocalDate.parse(semesterStart);
            LocalDate today = LocalDate.now();
            long daysBetween = ChronoUnit.DAYS.between(startDate, today);
            int week = (int) (daysBetween / 7) + 1;
            return Math.max(1, Math.min(week, ScheduleConstants.MAX_TEACHING_WEEK));
        } catch (Exception e) {
            return 1;
        }
    }

    public CourseRepository getRepository() {
        return repository;
    }
}
