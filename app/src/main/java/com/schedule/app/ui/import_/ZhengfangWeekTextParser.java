package com.schedule.app.ui.import_;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正方课表周次字段（如 zcd）解析，与 Python 导出 Markdown 所用语义一致。
 */
public final class ZhengfangWeekTextParser {

    private static final Pattern WEEK_RANGE_PAT =
            Pattern.compile("^(\\d+)-(\\d+)周(?:\\(([单双])\\))?$");
    private static final Pattern WEEK_SINGLE_PAT =
            Pattern.compile("^(\\d+)周(?:\\(([单双])\\))?$");

    public static final class WeekSpan {
        public final int startWeek;
        public final int endWeek;
        public final int weekType;

        public WeekSpan(int startWeek, int endWeek, int weekType) {
            this.startWeek = startWeek;
            this.endWeek = endWeek;
            this.weekType = weekType;
        }
    }

    private ZhengfangWeekTextParser() {}

    public static List<WeekSpan> parseWeekSpans(String zcd) {
        List<WeekSpan> list = new ArrayList<>();
        if (zcd == null || zcd.isBlank()) {
            return list;
        }
        String[] chunks = zcd.trim().split("[,\uFF0C]");
        for (String chunk : chunks) {
            String p = chunk.trim();
            if (p.isEmpty()) {
                continue;
            }
            Matcher mr = WEEK_RANGE_PAT.matcher(p);
            if (mr.matches()) {
                int a = Integer.parseInt(mr.group(1));
                int b = Integer.parseInt(mr.group(2));
                int wt = weekTypeFromSuffix(mr.group(3));
                if (a > b) {
                    int t = a;
                    a = b;
                    b = t;
                }
                list.add(new WeekSpan(a, b, wt));
                continue;
            }
            Matcher ms = WEEK_SINGLE_PAT.matcher(p);
            if (ms.matches()) {
                int w = Integer.parseInt(ms.group(1));
                int wt = weekTypeFromSuffix(ms.group(2));
                list.add(new WeekSpan(w, w, wt));
            }
        }
        return list;
    }

    private static int weekTypeFromSuffix(String s) {
        if (s == null) {
            return 0;
        }
        if ("单".equals(s)) {
            return 1;
        }
        if ("双".equals(s)) {
            return 2;
        }
        return 0;
    }
}
