package testapp.silencertestapp;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InstrumentedMainActivityTest {

    private MainActivity testMain;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Before
    public void createActivity(){
        testMain = mActivityRule.getActivity();
    }

    @Test
    public void checkExtraIsReturned(){
        String[] ssids = testMain.getSSID("First network");
        assertEquals("first network is correct", ssids[0], "First network");

    }

    @Test
    public void checkSetConvertedToString(){
        Set<Integer> temp = new HashSet<>();
        temp.add(1);
        temp.add(2);
        temp.add(3);
        temp.add(4);
        temp.add(7);
        String tempString = testMain.setToString(temp);
        String[] tempArray = tempString.split(",");
        Arrays.sort(tempArray);
        String[] expectedResult = {"1","2","3","4","7"};
        assertArrayEquals("Should return a string \"1,2,3,4,7\"", tempArray, expectedResult);
    }
}