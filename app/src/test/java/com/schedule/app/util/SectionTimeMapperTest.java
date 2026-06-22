package com.schedule.app.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@link SectionTimeMapper} 模板计算逻辑测试。
 * 注意：纯逻辑测试不依赖 Android Context，只测试 parseHHmm 等纯函数。
 * 涉及 SharedPreferences 的方法需要 instrumented test 或 mock。
 */
public class SectionTimeMapperTest {

    @Test
    public void defaultValues_areReasonable() {
        // 验证常量默认值
        assertEquals("section_template_first_start", SectionTimeMapper.KEY_FIRST_START);
        assertEquals("section_template_duration", SectionTimeMapper.KEY_DURATION);
        assertEquals("section_template_break", SectionTimeMapper.KEY_BREAK);
        assertEquals(16, SectionTimeMapper.MAX_SECTIONS);
    }

    @Test
    public void defaultStartTime_inValidRange() {
        // 默认开始时间 08:00
        String start = SectionTimeMapper.getDefaultStartTime(1);
        assertEquals("08:00", start);
    }

    @Test
    public void defaultEndTime_inValidRange() {
        // 默认结束时间 08:50
        String end = SectionTimeMapper.getDefaultEndTime(1);
        assertEquals("08:50", end);
    }

    @Test
    public void sectionOutOfRange_returnsZeroTime() {
        assertEquals("00:00", SectionTimeMapper.getDefaultStartTime(0));
        assertEquals("00:00", SectionTimeMapper.getDefaultStartTime(17));
        assertEquals("00:00", SectionTimeMapper.getDefaultEndTime(0));
        assertEquals("00:00", SectionTimeMapper.getDefaultEndTime(17));
    }

    @Test
    public void allDefaultTimes_validHHmmFormat() {
        for (int i = 1; i <= SectionTimeMapper.MAX_SECTIONS; i++) {
            String start = SectionTimeMapper.getDefaultStartTime(i);
            String end = SectionTimeMapper.getDefaultEndTime(i);
            // 格式 HH:mm
            assertTrue("Start time format invalid for section " + i,
                    start.matches("\\d{2}:\\d{2}"));
            assertTrue("End time format invalid for section " + i,
                    end.matches("\\d{2}:\\d{2}"));
            // start < end
            assertTrue("Start should be before end for section " + i,
                    start.compareTo(end) < 0);
        }
    }
}
