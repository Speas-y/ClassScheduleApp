package com.schedule.app.data.db;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.schedule.app.data.entity.Course;

import java.util.List;

/** 课程表的 CRUD；同步查询供闹钟调度、导入等在后台线程使用。 */
@Dao
public interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSection")
    LiveData<List<Course>> getAllCourses();

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSection")
    List<Course> getAllCoursesSync();

    @Nullable
    @Query("SELECT * FROM courses WHERE id = :id")
    Course getCourseById(int id);

    @Insert
    long insert(Course course);

    @Insert
    void insertAll(List<Course> courses);

    @Update
    void update(Course course);

    @Delete
    void delete(Course course);

    @Query("DELETE FROM courses")
    void deleteAll();

    /**
     * 查重：课程名 + 星期 + 起止节次完全一致视为重复。
     */
    @Nullable
    @Query("SELECT * FROM courses WHERE courseName = :name AND dayOfWeek = :day AND startSection = :start AND endSection = :end LIMIT 1")
    Course findDuplicate(String name, int day, int start, int end);

    /**
     * 查冲突：同一天、节次区间有交集的所有课程。
     */
    @Query("SELECT * FROM courses WHERE dayOfWeek = :day AND startSection <= :endSec AND endSection >= :startSec")
    List<Course> findConflicting(int day, int startSec, int endSec);

    /**
     * 合并导入：在单个事务中逐条查重并插入，返回 [新增数, 跳过数]。
     */
    @Transaction
    default int[] mergeInsert(List<Course> courses) {
        int added = 0;
        int skipped = 0;
        for (Course course : courses) {
            Course existing = findDuplicate(
                    course.getCourseName(),
                    course.getDayOfWeek(),
                    course.getStartSection(),
                    course.getEndSection());
            if (existing != null) {
                skipped++;
            } else {
                insert(course);
                added++;
            }
        }
        return new int[]{added, skipped};
    }
}
