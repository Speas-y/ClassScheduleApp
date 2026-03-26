package com.schedule.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
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
}
