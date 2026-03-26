package com.schedule.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScheduleConstantsTest {

    @Test
    public void maxTeachingWeek_matchesViewModel() {
        assertEquals(30, ScheduleConstants.MAX_TEACHING_WEEK);
    }
}
