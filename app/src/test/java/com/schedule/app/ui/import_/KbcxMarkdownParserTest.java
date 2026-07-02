package com.schedule.app.ui.import_;

import com.schedule.app.data.entity.Course;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/** 测试外部脚本导出的 Markdown 课表解析。 */
public class KbcxMarkdownParserTest {

    @Test
    public void parse_markdownTableWithMultipleWeekSpans() {
        String markdown = """
                # 课表

                ## 按星期 · 节次
                | 星期 | 节次 | 周次 | 课程 | 备注 | 学分 | 教师 | 教室 | 教学楼 | 校区 |
                | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
                | 星期一 | 1-2节 | 1-16周(单),18周 | 高等数学 |  |  | 张三 | 101 | A楼 | 主校区 |
                """;

        List<Course> courses = new KbcxMarkdownParser().parse(markdown);

        assertEquals(2, courses.size());
        assertEquals("高等数学", courses.get(0).getCourseName());
        assertEquals(1, courses.get(0).getDayOfWeek());
        assertEquals(1, courses.get(0).getStartSection());
        assertEquals(2, courses.get(0).getEndSection());
        assertEquals(1, courses.get(0).getWeekType());
        assertEquals("主校区 A楼 101", courses.get(0).getLocation());
        assertEquals(18, courses.get(1).getStartWeek());
        assertEquals(18, courses.get(1).getEndWeek());
    }
}
