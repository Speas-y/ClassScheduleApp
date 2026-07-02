package com.schedule.app.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 课程卡片颜色分配：提供预设调色板和按课程名称自动分配颜色的功能。
 */
public final class CourseColorPalette {

    private CourseColorPalette() {}

    public static final int[] PRESET_COLORS = {
            0xFF4FC3F7,
            0xFF81C784,
            0xFFFFB74D,
            0xFFE57373,
            0xFFBA68C8,
            0xFF4DD0E1,
            0xFFFFD54F,
            0xFFF06292,
            0xFFAED581,
            0xFF7986CB,
            0xFFFF8A65,
            0xFFA1887F,
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
