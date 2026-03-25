package com.schedule.app.ui.import_;

import android.graphics.Color;

import com.schedule.app.data.entity.Course;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正方教务课表接口 {@code xskbcx_cxXsgrkb.html} 返回的 JSON（含 kbList），
 * 与 Python {@code fetch_kbcx.py} 写入的 {@code kbcx_data.json} 结构一致。
 */
public class ZhengfangKbListJsonParser {

    private static final int[] CARD_COLORS = {
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

    private static final Pattern JCOR_RANGE = Pattern.compile("(\\d+)-(\\d+)");

    private final Map<String, Integer> colorByCourseName = new HashMap<>();
    private int colorRotor = 0;

    public List<Course> parse(String jsonText) {
        List<Course> out = new ArrayList<>();
        if (jsonText == null || jsonText.isBlank()) {
            return out;
        }
        String trimmed = jsonText.trim();
        if (!trimmed.startsWith("{")) {
            return out;
        }

        try {
            JSONObject root = new JSONObject(trimmed);
            JSONArray kbList = root.optJSONArray("kbList");
            if (kbList == null || kbList.length() == 0) {
                return out;
            }

            for (int i = 0; i < kbList.length(); i++) {
                JSONObject row = kbList.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String courseName = row.optString("kcmc", "").trim();
                if (courseName.isEmpty()) {
                    continue;
                }

                int day = row.optInt("xqj", -1);
                if (day < 1 || day > 7) {
                    String xqjStr = row.optString("xqj", "").trim();
                    try {
                        day = Integer.parseInt(xqjStr);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    if (day < 1 || day > 7) {
                        continue;
                    }
                }

                int[] sections = parseSections(
                        row.optString("jcor", ""),
                        row.optString("jc", ""));
                if (sections == null) {
                    continue;
                }

                String teacher = row.optString("xm", "").trim();
                String location = buildLocation(
                        row.optString("xqmc", "").trim(),
                        row.optString("lh", "").trim(),
                        row.optString("cdmc", "").trim());

                List<ZhengfangWeekTextParser.WeekSpan> spans =
                        ZhengfangWeekTextParser.parseWeekSpans(row.optString("zcd", ""));
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
        } catch (Exception ignored) {
            return new ArrayList<>();
        }

        return out;
    }

    private int colorForCourseName(String courseName) {
        Integer existing = colorByCourseName.get(courseName);
        if (existing != null) {
            return existing;
        }
        int c = CARD_COLORS[colorRotor % CARD_COLORS.length];
        colorRotor++;
        colorByCourseName.put(courseName, c);
        return c;
    }

    private static int[] parseSections(String jcor, String jcFallback) {
        int[] fromJcor = parseJcorString(jcor);
        if (fromJcor != null) {
            return fromJcor;
        }
        String jc = jcFallback == null ? "" : jcFallback.replace("节", "").trim();
        return parseJcorString(jc);
    }

    private static int[] parseJcorString(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        Matcher m = JCOR_RANGE.matcher(s);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            if (a > b) {
                int t = a;
                a = b;
                b = t;
            }
            return new int[]{a, b};
        }
        if (s.matches("\\d+")) {
            int n = Integer.parseInt(s);
            return new int[]{n, n};
        }
        return null;
    }

    private static String buildLocation(String campus, String building, String room) {
        StringBuilder sb = new StringBuilder();
        appendChunk(sb, campus);
        appendChunk(sb, building);
        appendChunk(sb, room);
        String str = sb.toString().trim();
        return str.isEmpty() ? "待定" : str;
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
