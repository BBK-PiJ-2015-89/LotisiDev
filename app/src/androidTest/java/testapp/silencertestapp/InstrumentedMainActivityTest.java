package testapp.silencertestapp;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InstrumentedMainActivityTest {

    private MainActivity testMain;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<MainActivity>(MainActivity.class);

    @Before
    public void createActivity(){
        testMain = mActivityRule.getActivity();
    }

    @Test
    public void checkFancyStuff(){
        String[] ssids = testMain.getSSID("First network");
        assertEquals("first network is correct", ssids[0], "First network");

    }
}