package com.schedule.app.ui.import_;

import com.schedule.app.data.entity.Course;

import java.util.Collections;
import java.util.List;

/** 导入解析结果：包含解析出的课程和用于 UI 提示的来源说明。 */
public class ImportResult {

    private final List<Course> courses;
    private final String methodHint;

    public ImportResult(List<Course> courses, String methodHint) {
        this.courses = Collections.unmodifiableList(courses);
        this.methodHint = methodHint;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public String getMethodHint() {
        return methodHint;
    }
}
