package com.schedule.app.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.schedule.app.data.db.AppDatabase;
import com.schedule.app.data.db.CourseDao;
import com.schedule.app.data.entity.Course;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据访问封装：向 UI 暴露 LiveData，写操作走单线程 Executor 避免主线程访问数据库。
 * 全进程单例，避免多处 new 重复占用线程池。
 */
public class CourseRepository {

    private static volatile CourseRepository INSTANCE;

    public static CourseRepository getInstance(@NonNull Application application) {
        Application app = (Application) application.getApplicationContext();
        if (INSTANCE == null) {
            synchronized (CourseRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CourseRepository(app);
                }
            }
        }
        return INSTANCE;
    }

    private final CourseDao courseDao;
    private final LiveData<List<Course>> allCourses;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CourseRepository(@NonNull Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        courseDao = db.courseDao();
        allCourses = courseDao.getAllCourses();
    }

    public LiveData<List<Course>> getAllCourses() {
        return allCourses;
    }

    @WorkerThread
    public List<Course> getAllCoursesSync() {
        return courseDao.getAllCoursesSync();
    }

    @WorkerThread
    @Nullable
    public Course getCourseById(int id) {
        return courseDao.getCourseById(id);
    }

    public void insert(@NonNull Course course) {
        executor.execute(() -> courseDao.insert(course));
    }

    public void insertAll(@NonNull List<Course> courses) {
        executor.execute(() -> courseDao.insertAll(courses));
    }

    public void update(@NonNull Course course) {
        executor.execute(() -> courseDao.update(course));
    }

    public void delete(@NonNull Course course) {
        executor.execute(() -> courseDao.delete(course));
    }

    public void deleteAll() {
        executor.execute(courseDao::deleteAll);
    }

    /**
     * 插入全部课程并在完成后回调（回调在主线程执行）。
     */
    public void insertAllAndCallback(@NonNull List<Course> courses, @Nullable Runnable onComplete) {
        executor.execute(() -> {
            courseDao.insertAll(courses);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    /**
     * 合并导入：逐条查重（courseName + dayOfWeek + startSection + endSection），
     * 已存在则跳过，不存在则插入。整个操作在单个事务中执行以保证性能。
     * 回调在主线程执行。
     *
     * @param courses    待导入课程列表
     * @param onComplete 回调参数 [新增数, 跳过数]
     */
    public void mergeCourses(@NonNull List<Course> courses, @Nullable MergeCallback onComplete) {
        executor.execute(() -> {
            int[] result = courseDao.mergeInsert(courses);
            int added = result[0];
            int skipped = result[1];
            if (onComplete != null) {
                mainHandler.post(() -> onComplete.onComplete(added, skipped));
            }
        });
    }

    /** 查找冲突课程（同一天、节次区间有交集）。 */
    @WorkerThread
    @NonNull
    public List<Course> findConflicting(int dayOfWeek, int startSection, int endSection) {
        return courseDao.findConflicting(dayOfWeek, startSection, endSection);
    }

    /** 合并导入结果回调。 */
    public interface MergeCallback {
        void onComplete(int added, int skipped);
    }
}
