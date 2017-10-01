package testapp.silencertestapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by graemewilkinson
 */

public class TimeHandlingTest {


    @Test
    public void dismantleFancyTimeWorksStandard(){
        assertEquals("Should return non-fancy time of 07:00 should return 700", 700, TimeHandling.dismantleFancyTime("07:00"));

    }

    @Test
    public void dismantleFancyTimeWorksEdge(){
        assertEquals("Should return non-fancy time of 0:00 should return 0", 0, TimeHandling.dismantleFancyTime("00:00"));

    }

    @Test
    public void dismantleFancyTimeWorksEdgeJustMin(){
        assertEquals("Should return non-fancy time of 0:54 should return 54", 54, TimeHandling.dismantleFancyTime("00:54"));
    }

    @Test
    public void dismantleFancyTimeWorksEdgeHourHigh(){
        assertEquals("Should return non-fancy time of 23:54 should return 54", 2354, TimeHandling.dismantleFancyTime("23:54"));
    }

    @Test
    public void getHourBasic(){
        assertEquals("should return 23 from 23:54", 23, TimeHandling.getHour("23:54"));
        assertEquals("should return 7 from 07:23", 7, TimeHandling.getHour("07:23"));
        assertEquals("should return 0 from 00:23", 0, TimeHandling.getHour("00:23"));
    }

    @Test
    public void getMin(){
        assertEquals("should return 54 from 23:54", 54, TimeHandling.getMinute("23:54"));
        assertEquals("should return 0 from 23:00", 0, TimeHandling.getMinute("23:00"));
        assertEquals("should return 30 from 23:30", 30, TimeHandling.getMinute("23:30"));
        assertEquals("should return 7 from 23:07", 7, TimeHandling.getMinute("23:07"));
        assertEquals("should return 0 from 00:00", 0, TimeHandling.getMinute("00:00"));
    }

    @Test
    public void displayFancyTimeTest(){
        assertEquals("should return 07:07 from 707", "07:07", TimeHandling.createFancyTime(707));
        assertEquals("should return 00:07 from 7", "00:07", TimeHandling.createFancyTime(7));
        assertEquals("should return 23:00 from 2300", "23:00", TimeHandling .createFancyTime(2300));
    }

}
