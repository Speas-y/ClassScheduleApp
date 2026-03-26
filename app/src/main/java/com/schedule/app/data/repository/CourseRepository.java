package com.schedule.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.schedule.app.data.db.AppDatabase;
import com.schedule.app.data.db.CourseDao;
import com.schedule.app.data.entity.Course;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据访问封装：向 UI 暴露 LiveData，写操作走单线程 Executor 避免主线程访问数据库。
 */
public class CourseRepository {

    private final CourseDao courseDao;
    private final LiveData<List<Course>> allCourses;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CourseRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        courseDao = db.courseDao();
        allCourses = courseDao.getAllCourses();
    }

    public LiveData<List<Course>> getAllCourses() {
        return allCourses;
    }

    public List<Course> getAllCoursesSync() {
        return courseDao.getAllCoursesSync();
    }

    public Course getCourseById(int id) {
        return courseDao.getCourseById(id);
    }

    public void insert(Course course) {
        executor.execute(() -> courseDao.insert(course));
    }

    public void insertAll(List<Course> courses) {
        executor.execute(() -> courseDao.insertAll(courses));
    }

    public void update(Course course) {
        executor.execute(() -> courseDao.update(course));
    }

    public void delete(Course course) {
        executor.execute(() -> courseDao.delete(course));
    }

    public void deleteAll() {
        executor.execute(courseDao::deleteAll);
    }

    public void insertAllAndCallback(List<Course> courses, Runnable onComplete) {
        executor.execute(() -> {
            courseDao.insertAll(courses);
            if (onComplete != null) onComplete.run();
        });
    }
}
