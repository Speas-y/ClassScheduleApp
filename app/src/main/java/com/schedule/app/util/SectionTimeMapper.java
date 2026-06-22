package com.schedule.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * 节次与上下课时间的映射。
 *
 * <p>新版采用「3 参数模板 + 手动覆盖」机制：
 * <ul>
 *   <li>模板参数：第一节课开始时间、每节课时长（分钟）、课间休息时长（分钟）</li>
 *   <li>从第 1 节开始按「上课 N 分钟 + 休息 M 分钟」逐节推算</li>
 *   <li>用户可在预览中点击任意一节设定该节开始时间，后续节次自动级联推算</li>
 * </ul>
 *
 * <p>旧版逐节覆盖键 {@code section_X_start} / {@code section_X_end} 仍兼容读取，
 * 但新代码统一走 {@link #calcAllSections(Context)}。
 */
public class SectionTimeMapper {

    public static final int MAX_SECTIONS = 16;

    /** 模板 SharedPreferences 键 */
    public static final String KEY_FIRST_START = "section_template_first_start";
    public static final String KEY_DURATION = "section_template_duration";
    public static final String KEY_BREAK = "section_template_break";

    /** 默认值 */
    private static final String DEFAULT_FIRST_START = "08:00";
    private static final int DEFAULT_DURATION = 50;
    private static final int DEFAULT_BREAK = 10;

    /** 兜底旧表，仅在 SharedPreferences 完全缺失时使用 */
    private static final String[][] LEGACY_DEFAULTS = {
            {"08:00", "08:50"}, {"08:55", "09:45"}, {"10:05", "10:55"}, {"11:00", "11:50"},
            {"14:00", "14:50"}, {"14:55", "15:45"}, {"16:05", "16:55"}, {"17:00", "17:50"},
            {"19:00", "19:50"}, {"19:55", "20:45"}, {"20:55", "21:45"}, {"21:50", "22:40"},
            {"08:30", "09:10"}, {"09:15", "09:55"}, {"10:00", "10:40"}, {"10:45", "11:25"},
    };

    // ───────────────────────── 公开接口 ─────────────────────────

    public static int getTotalSections(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Integer.parseInt(prefs.getString("total_sections", "12"));
        } catch (NumberFormatException e) {
            return 12;
        }
    }

    /** 获取第 section 节的开始时间 HH:mm */
    public static String getStartTime(Context context, int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        return calcAllSections(context)[section - 1][0];
    }

    /** 获取第 section 节的结束时间 HH:mm */
    public static String getEndTime(Context context, int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        return calcAllSections(context)[section - 1][1];
    }

    public static int getStartHour(Context context, int section) {
        return parseTimeHourMinute(getStartTime(context, section), section)[0];
    }

    public static int getStartMinute(Context context, int section) {
        return parseTimeHourMinute(getStartTime(context, section), section)[1];
    }

    public static String getSectionRangeDisplay(Context context, int startSection, int endSection) {
        return getStartTime(context, startSection) + " - " + getEndTime(context, endSection);
    }

    // ───────────────────────── 模板参数读写 ─────────────────────────

    public static String getFirstStartTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_FIRST_START, DEFAULT_FIRST_START);
    }

    public static int getDuration(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_DURATION, DEFAULT_DURATION);
    }

    public static int getBreakMinutes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_BREAK, DEFAULT_BREAK);
    }

    /** 保存模板参数 */
    public static void saveTemplate(Context context, String firstStart, int duration, int breakMin) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(KEY_FIRST_START, firstStart)
                .putInt(KEY_DURATION, duration)
                .putInt(KEY_BREAK, breakMin)
                .apply();
    }

    /** 保存单节手动覆盖的开始时间（仅 start，end 由模板推算） */
    public static void saveSectionOverride(Context context, int section, String startTime) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("section_" + section + "_start", startTime)
                .remove("section_" + section + "_end") // end 不再单独存储
                .apply();
    }

    /** 清除单节手动覆盖 */
    public static void clearSectionOverride(Context context, int section) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove("section_" + section + "_start")
                .remove("section_" + section + "_end")
                .apply();
    }

    /** 恢复所有默认值：清除模板参数 + 所有逐节覆盖 */
    public static void resetAll(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(KEY_FIRST_START);
        editor.remove(KEY_DURATION);
        editor.remove(KEY_BREAK);
        for (int i = 1; i <= MAX_SECTIONS; i++) {
            editor.remove("section_" + i + "_start");
            editor.remove("section_" + i + "_end");
        }
        editor.apply();
    }

    /** 判断某节次是否有手动覆盖 */
    public static boolean isOverridden(Context context, int section) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .contains("section_" + section + "_start");
    }

    // ───────────────────────── 核心计算 ─────────────────────────

    /**
     * 根据模板参数 + 手动覆盖，计算所有节次的开始/结束时间。
     *
     * <p>算法：
     * 从第 1 节开始，如果该节有手动覆盖则以覆盖值为开始时间，
     * 否则如果该节是第 1 节则用模板 first_start，
     * 否则以上一节结束 + break 分钟为开始时间。
     * 结束时间 = 开始时间 + duration 分钟。
     *
     * @return 数组长度为 MAX_SECTIONS，每项 [0]=start "HH:mm" [1]=end "HH:mm"
     */
    public static String[][] calcAllSections(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String firstStart = prefs.getString(KEY_FIRST_START, DEFAULT_FIRST_START);
        int duration;
        int breakMin;
        try {
            duration = prefs.getInt(KEY_DURATION, DEFAULT_DURATION);
        } catch (ClassCastException e) {
            // 兼容旧版可能存为 String
            try {
                duration = Integer.parseInt(prefs.getString(KEY_DURATION, String.valueOf(DEFAULT_DURATION)));
            } catch (Exception e2) {
                duration = DEFAULT_DURATION;
            }
        }
        try {
            breakMin = prefs.getInt(KEY_BREAK, DEFAULT_BREAK);
        } catch (ClassCastException e) {
            try {
                breakMin = Integer.parseInt(prefs.getString(KEY_BREAK, String.valueOf(DEFAULT_BREAK)));
            } catch (Exception e2) {
                breakMin = DEFAULT_BREAK;
            }
        }

        int totalSections = getTotalSections(context);
        String[][] result = new String[MAX_SECTIONS][2];

        int curH, curM;
        int[] parsed = parseHHmm(firstStart);
        curH = parsed[0];
        curM = parsed[1];

        for (int i = 0; i < MAX_SECTIONS; i++) {
            int section = i + 1;

            // 检查手动覆盖
            String overrideKey = "section_" + section + "_start";
            if (prefs.contains(overrideKey)) {
                String ov = prefs.getString(overrideKey, null);
                if (ov != null) {
                    int[] ovParsed = parseHHmmOrNull(ov);
                    if (ovParsed != null) {
                        curH = ovParsed[0];
                        curM = ovParsed[1];
                    }
                }
            }

            String startStr = String.format("%02d:%02d", curH, curM);

            // 计算结束时间
            int endTotal = curH * 60 + curM + duration;
            int endH = endTotal / 60;
            int endM = endTotal % 60;
            String endStr = String.format("%02d:%02d", endH, endM);

            result[i][0] = startStr;
            result[i][1] = endStr;

            // 下一节的开始 = 本节结束 + break
            int nextTotal = endH * 60 + endM + breakMin;
            curH = nextTotal / 60;
            curM = nextTotal % 60;
        }

        return result;
    }

    // ───────────────────────── 内部工具 ─────────────────────────

    private static int[] parseHHmm(String time) {
        int[] r = parseHHmmOrNull(time);
        return r != null ? r : new int[]{8, 0};
    }

    private static int[] parseHHmmOrNull(String time) {
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
        return null;
    }

    private static int[] parseTimeHourMinute(String time, int section) {
        int[] r = parseHHmmOrNull(time);
        if (r != null) return r;
        // 回退到旧表
        String def = (section >= 1 && section <= MAX_SECTIONS)
                ? LEGACY_DEFAULTS[section - 1][0] : "08:00";
        return parseHHmm(def);
    }

    // ───────────────────────── 旧接口兼容 ─────────────────────────

    public static String getDefaultStartTime(int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        return LEGACY_DEFAULTS[section - 1][0];
    }

    public static String getDefaultEndTime(int section) {
        if (section < 1 || section > MAX_SECTIONS) return "00:00";
        return LEGACY_DEFAULTS[section - 1][1];
    }
}
