package com.schedule.app.ui.import_;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 测试正方教务系统周次解析逻辑
 */
public class ZhengfangWeekTextParserTest {

    @Test
    public void parseWeekSpans_simpleRange() {
        // 测试简单周次范围: "1-16周"
        var spans = ZhengfangWeekTextParser.parseWeekSpans("1-16周");
        assertEquals(1, spans.size());
        assertEquals(1, spans.get(0).startWeek);
        assertEquals(16, spans.get(0).endWeek);
        assertEquals(0, spans.get(0).weekType); // 每周
    }

    @Test
    public void parseWeekSpans_singleWeek() {
        // 测试单周: "9-11周(单)"
        var spans = ZhengfangWeekTextParser.parseWeekSpans("9-11周(单)");
        assertEquals(1, spans.size());
        assertEquals(9, spans.get(0).startWeek);
        assertEquals(11, spans.get(0).endWeek);
        assertEquals(1, spans.get(0).weekType); // 单周
    }

    @Test
    public void parseWeekSpans_multipleRanges() {
        // 测试多个周次范围: "9-11周(单),12-16周"
        var spans = ZhengfangWeekTextParser.parseWeekSpans("9-11周(单),12-16周");
        assertEquals(2, spans.size());
        
        // 第一个范围: 9-11周(单)
        assertEquals(9, spans.get(0).startWeek);
        assertEquals(11, spans.get(0).endWeek);
        assertEquals(1, spans.get(0).weekType);
        
        // 第二个范围: 12-16周
        assertEquals(12, spans.get(1).startWeek);
        assertEquals(16, spans.get(1).endWeek);
        assertEquals(0, spans.get(1).weekType);
    }

    @Test
    public void parseWeekSpans_empty() {
        // 测试空字符串
        var spans = ZhengfangWeekTextParser.parseWeekSpans("");
        assertTrue(spans.isEmpty());
    }

    @Test
    public void parseWeekSpans_null() {
        // 测试null
        var spans = ZhengfangWeekTextParser.parseWeekSpans(null);
        assertTrue(spans.isEmpty());
    }

    @Test
    public void parseWeekSpans_evenWeek() {
        // 测试双周: "2-12周(双)"
        var spans = ZhengfangWeekTextParser.parseWeekSpans("2-12周(双)");
        assertEquals(1, spans.size());
        assertEquals(2, spans.get(0).startWeek);
        assertEquals(12, spans.get(0).endWeek);
        assertEquals(2, spans.get(0).weekType); // 双周
    }

    @Test
    public void parseWeekSpans_complexFormat() {
        // 测试复杂格式: "1-8周,10-16周(单)"
        var spans = ZhengfangWeekTextParser.parseWeekSpans("1-8周,10-16周(单)");
        assertEquals(2, spans.size());
        
        // 第一个范围: 1-8周
        assertEquals(1, spans.get(0).startWeek);
        assertEquals(8, spans.get(0).endWeek);
        assertEquals(0, spans.get(0).weekType);
        
        // 第二个范围: 10-16周(单)
        assertEquals(10, spans.get(1).startWeek);
        assertEquals(16, spans.get(1).endWeek);
        assertEquals(1, spans.get(1).weekType);
    }
}
