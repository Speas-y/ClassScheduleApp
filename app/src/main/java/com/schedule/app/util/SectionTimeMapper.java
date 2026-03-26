package com.schedule.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * 节次与上下课时间的映射：默认 12～16 节表在 {@link #DEFAULT_TIMES}，
 * 用户可在设置里覆盖或通过 SharedPreferences 的 {@code total_sections} 扩展行数。
 */
public class SectionTimeMapper {

    public static final int MAX_SECTIONS = 16;

    private static final String[][] DEFAULT_TIMES = {
            {"08:00", "08:50"},   // 1
            {"08:55", "09:45"},   // 2
            {"10:05", "10:55"},   // 3
            {"11:00", "11:50"},   // 4
            {"14:00", "14:50"},   // 5
            {"14:55", "15:45"},   // 6
            {"16:05", "16:55"},   // 7
            {"17:00", "17:50"},   // 8
            {"19:00", "19:50"},   // 9
            {"19:55", "20:45"},   // 10
            {"20:55", "21:45"},   // 11
            {"21:50", "22:40"},   // 12
            {"08:30", "09:10"},   // 13 (fallback for extended)
            {"09:15", "09:55"},   // 14
            {"10:00", "10:40"},   // 15
            {"10:45", "11:25"},   // 16
    };

    public static int getTotalSections(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Integer.parseInt(prefs.getString("total_sections", "12"));
        } catch (NumberFormatException e) {
            return 12;
        }
    }

    public static String getStartTime(Context context, int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "section_" + section + "_start";
        return prefs.getString(key, DEFAULT_TIMES[section - 1][0]);
    }

    public static String getEndTime(Context context, int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "section_" + section + "_end";
        return prefs.getString(key, DEFAULT_TIMES[section - 1][1]);
    }

    public static int getStartHour(Context context, int section) {
        return parseTimeHourMinute(getStartTime(context, section), section)[0];
    }

    public static int getStartMinute(Context context, int section) {
        return parseTimeHourMinute(getStartTime(context, section), section)[1];
    }

    /**
     * 解析 HH:mm；非法或用户自定义异常时回退到默认表，避免闹铃与绘制崩溃。
     */
    private static int[] parseTimeHourMinute(String time, int section) {
        try {
            if (time != null && time.contains(":")) {
                String[] parts = time.trim().split(":");
                if (parts.length >= 2) {
                    int h = Integer.parseInt(parts[0].trim());
                    int m = Integer.parseInt(parts[1].trim());
                    if (h >= 0 && h <= 23 && m >= 0 && m <= 59) {
                        return new int[]{h, m};
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }
        String def = (section >= 1 && section <= MAX_SECTIONS)
                ? DEFAULT_TIMES[section - 1][0] : "08:00";
        try {
            String[] p = def.split(":");
            return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])};
        } catch (Exception e) {
            return new int[]{8, 0};
        }
    }

    public static String getDefaultStartTime(int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        return DEFAULT_TIMES[section - 1][0];
    }

    public static String getDefaultEndTime(int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        return DEFAULT_TIMES[section - 1][1];
    }

    public static String getSectionRangeDisplay(Context context, int startSection, int endSection) {
        return getStartTime(context, startSection) + " - " + getEndTime(context, endSection);
    }
}
