package com.schedule.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

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
        String time = getStartTime(context, section);
        return Integer.parseInt(time.split(":")[0]);
    }

    public static int getStartMinute(Context context, int section) {
        String time = getStartTime(context, section);
        return Integer.parseInt(time.split(":")[1]);
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
