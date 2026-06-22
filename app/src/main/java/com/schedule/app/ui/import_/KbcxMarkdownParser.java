package com.schedule.app.ui.import_;

import com.schedule.app.util.CourseColorPalette;

import com.schedule.app.data.entity.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 {@code format_kbcx.py} 生成的 {@code kbcx_schedule.md} 中
 * 「按星期 · 节次」表格，转为 {@link Course} 列表。
 */
public class KbcxMarkdownParser {

    private static final Pattern SECTION_PAT = Pattern.compile("^(\\d+)-(\\d+)节?$");

    private final CourseColorPalette.Allocator colorAllocator = new CourseColorPalette.Allocator();

    public List<Course> parse(String markdown) {
        List<Course> out = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return out;
        }

        int secHeading = markdown.indexOf("## 按星期");
        if (secHeading < 0) {
            return out;
        }

        int lineStart = markdown.indexOf('\n', secHeading);
        String rest = lineStart >= 0 ? markdown.substring(lineStart + 1) : "";

        boolean headerSkipped = false;
        for (String rawLine : rest.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("## ")) {
                break;
            }
            if (!line.startsWith("|")) {
                continue;
            }
            if (line.contains("---")) {
                continue;
            }

            List<String> cells = splitMarkdownTableRow(line);
            if (cells.size() < 4) {
                continue;
            }

            if (!headerSkipped) {
                if (cells.get(0).contains("星期") || cells.get(1).contains("节次")) {
                    headerSkipped = true;
                }
                continue;
            }

            int day = chineseDayToNumber(cells.get(0));
            if (day < 1) {
                continue;
            }

            int[] sections = parseSections(cells.get(1));
            if (sections == null) {
                continue;
            }

            String weekText = cells.get(2);
            String courseName = cells.get(3).trim();
            if (courseName.isEmpty()) {
                continue;
            }

            String teacher = cells.size() > 6 ? cells.get(6).trim() : "";
            String room = cells.size() > 7 ? cells.get(7).trim() : "";
            String building = cells.size() > 8 ? cells.get(8).trim() : "";
            String campus = cells.size() > 9 ? cells.get(9).trim() : "";
            String location = buildLocation(campus, building, room);

            List<ZhengfangWeekTextParser.WeekSpan> spans =
                    ZhengfangWeekTextParser.parseWeekSpans(weekText);
            if (spans.isEmpty()) {
                spans.add(new ZhengfangWeekTextParser.WeekSpan(1, 20, 0));
            }

            int color = colorForCourseName(courseName);
            for (ZhengfangWeekTextParser.WeekSpan span : spans) {
                out.add(new Course(
                        courseName,
                        teacher,
                        location,
                        day,
                        sections[0],
                        sections[1],
                        span.startWeek,
                        span.endWeek,
                        span.weekType,
                        color
                ));
            }
        }

        return out;
    }

    private static List<String> splitMarkdownTableRow(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length() && line.charAt(i + 1) == '|') {
                cur.append('|');
                i++;
            } else if (c == '|') {
                parts.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString().trim());
        while (!parts.isEmpty() && parts.get(0).isEmpty()) {
            parts.remove(0);
        }
        while (!parts.isEmpty() && parts.get(parts.size() - 1).isEmpty()) {
            parts.remove(parts.size() - 1);
        }
        return parts;
    }

    private int colorForCourseName(String courseName) {
        return colorAllocator.getColor(courseName);
    }
    private static int chineseDayToNumber(String cell) {
        String s = cell.trim();
        switch (s) {
            case "星期一":
            case "周一":
                return 1;
            case "星期二":
            case "周二":
                return 2;
            case "星期三":
            case "周三":
                return 3;
            case "星期四":
            case "周四":
                return 4;
            case "星期五":
            case "周五":
                return 5;
            case "星期六":
            case "周六":
                return 6;
            case "星期日":
            case "星期天":
            case "周日":
                return 7;
            default:
                return -1;
        }
    }

    private static int[] parseSections(String cell) {
        String s = cell.trim();
        Matcher m = SECTION_PAT.matcher(s);
        if (!m.find()) {
            return null;
        }
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(2));
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return new int[]{a, b};
    }

    private static String buildLocation(String campus, String building, String room) {
        StringBuilder sb = new StringBuilder();
        appendChunk(sb, campus);
        appendChunk(sb, building);
        appendChunk(sb, room);
        String s = sb.toString().trim();
        return s.isEmpty() ? "待定" : s;
    }

    private static void appendChunk(StringBuilder sb, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(chunk);
    }

}
