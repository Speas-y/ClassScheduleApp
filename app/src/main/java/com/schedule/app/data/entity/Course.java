package com.schedule.app.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "courses")
public class Course {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String courseName;
    private String teacher;
    private String location;
    private int dayOfWeek;      // 1=周一 ... 7=周日
    private int startSection;   // 1-12
    private int endSection;     // 1-12
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
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getStartSection() { return startSection; }
    public void setStartSection(int startSection) { this.startSection = startSection; }

    public int getEndSection() { return endSection; }
    public void setEndSection(int endSection) { this.endSection = endSection; }

    public int getStartWeek() { return startWeek; }
    public void setStartWeek(int startWeek) { this.startWeek = startWeek; }

    public int getEndWeek() { return endWeek; }
    public void setEndWeek(int endWeek) { this.endWeek = endWeek; }

    public int getWeekType() { return weekType; }
    public void setWeekType(int weekType) { this.weekType = weekType; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public boolean isActiveInWeek(int week) {
        if (week < startWeek || week > endWeek) return false;
        if (weekType == 1 && week % 2 == 0) return false;
        if (weekType == 2 && week % 2 == 1) return false;
        return true;
    }
}
