package com.schedule.app.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * 单门课程在 Room 中的持久化模型。
 * 周次与单双周语义见 {@link #isActiveInWeek(int)}；节次与校区作息见 {@link com.schedule.app.util.SectionTimeMapper}。
 */
@Entity(tableName = "courses")
public class Course {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String courseName;
    private String teacher;
    private String location;
    private int dayOfWeek;      // 1=周一 ... 7=周日
    private int startSection;   // 1-16
    private int endSection;     // 1-16
    private int startWeek;
    private int endWeek;
    private int weekType;       // 0=每周, 1=单周, 2=双周
    private int color;

    public Course() {}

    @Ignore
    public Course(String courseName, String teacher, String location,
                  int dayOfWeek, int startSection, int endSection,
                  int startWeek, int endWeek, int weekType, int color) {
        this.courseName = courseName;
        this.teacher = teacher;
        this.location = location;
        this.dayOfWeek = dayOfWeek;
        this.startSection = startSection;
        this.endSection = endSection;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.weekType = weekType;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be 1-7, got " + dayOfWeek);
        }
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartSection() { return startSection; }
    public void setStartSection(int startSection) {
        if (startSection < 1) {
            throw new IllegalArgumentException("startSection must be >= 1, got " + startSection);
        }
        this.startSection = startSection;
    }

    public int getEndSection() { return endSection; }
    public void setEndSection(int endSection) {
        if (endSection < 1) {
            throw new IllegalArgumentException("endSection must be >= 1, got " + endSection);
        }
        this.endSection = endSection;
    }

    public int getStartWeek() { return startWeek; }
    public void setStartWeek(int startWeek) {
        if (startWeek < 1) {
            throw new IllegalArgumentException("startWeek must be >= 1, got " + startWeek);
        }
        this.startWeek = startWeek;
    }

    public int getEndWeek() { return endWeek; }
    public void setEndWeek(int endWeek) {
        if (endWeek < 1) {
            throw new IllegalArgumentException("endWeek must be >= 1, got " + endWeek);
        }
        this.endWeek = endWeek;
    }

    public int getWeekType() { return weekType; }
    public void setWeekType(int weekType) {
        if (weekType < 0 || weekType > 2) {
            throw new IllegalArgumentException("weekType must be 0/1/2, got " + weekType);
        }
        this.weekType = weekType;
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    /**
     * 判断该课程在指定教学周是否活跃。
     * 约定 week=1 为学期第一教学周（即「单周」），week=2 为第二教学周（即「双周」）。
     */
    public boolean isActiveInWeek(int week) {
        if (week < startWeek || week > endWeek) return false;
        if (weekType == 1 && week % 2 == 0) return false;  // 单周：排除偶数周
        if (weekType == 2 && week % 2 == 1) return false;  // 双周：排除奇数周
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return id != 0 && id == course.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", name='" + courseName + '\'' +
                ", day=" + dayOfWeek +
                ", sections=" + startSection + "-" + endSection +
                ", weeks=" + startWeek + "-" + endWeek +
                ", weekType=" + weekType +
                '}';
    }
}
