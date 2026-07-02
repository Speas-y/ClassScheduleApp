package com.schedule.app.ui.import_;

import com.schedule.app.data.entity.Course;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/** 测试正方 kbList JSON 导入解析。 */
public class ZhengfangKbListJsonParserTest {

    @Test
    public void parse_kbListJsonWithWeekSegments() {
        String json = """
                {
                  "kbList": [
                    {
                      "kcmc": "大学英语",
                      "xqj": "2",
                      "jcor": "3-4",
                      "xm": "李四",
                      "xqmc": "主校区",
                      "lh": "B楼",
                      "cdmc": "202",
                      "zcd": "1-8周,10-12周(双)"
                    }
                  ]
                }
                """;

        List<Course> courses = new ZhengfangKbListJsonParser().parse(json);

        assertEquals(2, courses.size());
        assertEquals("大学英语", courses.get(0).getCourseName());
        assertEquals(2, courses.get(0).getDayOfWeek());
        assertEquals(3, courses.get(0).getStartSection());
        assertEquals(4, courses.get(0).getEndSection());
        assertEquals(1, courses.get(0).getStartWeek());
        assertEquals(8, courses.get(0).getEndWeek());
        assertEquals(0, courses.get(0).getWeekType());
        assertEquals(10, courses.get(1).getStartWeek());
        assertEquals(12, courses.get(1).getEndWeek());
        assertEquals(2, courses.get(1).getWeekType());
        assertEquals("主校区 B楼 202", courses.get(0).getLocation());
    }
}
