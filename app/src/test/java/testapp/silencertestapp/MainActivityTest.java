package testapp.silencertestapp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "AndroidManifest.xml")
public class MainActivityTest {

    @Test
    public void shouldNotBeNull(){
        MainActivity activity = Robolectric.setupActivity(MainActivity.class);

    }
}