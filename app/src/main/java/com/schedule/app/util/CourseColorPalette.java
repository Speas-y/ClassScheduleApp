package com.schedule.app.util;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * 课程卡片颜色分配：提供预设调色板和按课程名称自动分配颜色的功能。
 */
public final class CourseColorPalette {

    private CourseColorPalette() {}

    public static final int[] PRESET_COLORS = {
            Color.parseColor("#4FC3F7"),
            Color.parseColor("#81C784"),
            Color.parseColor("#FFB74D"),
            Color.parseColor("#E57373"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#4DD0E1"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#F06292"),
            Color.parseColor("#AED581"),
            Color.parseColor("#7986CB"),
            Color.parseColor("#FF8A65"),
            Color.parseColor("#A1887F"),
    };

    /**
     * 为课程名称分配一致的颜色。相同名称总是返回相同颜色。
     */
    public static class Allocator {
        private final Map<String, Integer> colorByCourseName = new HashMap<>();
        private int colorRotor = 0;

        public int getColor(String courseName) {
            Integer existing = colorByCourseName.get(courseName);
            if (existing != null) {
                return existing;
            }
            int c = PRESET_COLORS[colorRotor % PRESET_COLORS.length];
            colorRotor++;
            colorByCourseName.put(courseName, c);
            return c;
        }
    }
}
