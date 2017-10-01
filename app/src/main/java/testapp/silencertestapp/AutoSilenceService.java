package testapp.silencertestapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by graemewilkinson on 01/08/2017.
 */

/**
 * Background service that is persistent and will restart on termination.
 *
 * Background service will run even when app is closed and will check the conditions of the system every 1 minute
 * and make changes if conditions are met.
 *
 * @author graemewilkinson
 *
 */
public class AutoSilenceService extends Service {
    public static final String WIFI_NAME_KEY = "_wifi_name";
    public static final String START_TIME_KEY = "_start_time";
    public static final String END_TIME_KEY = "_end_time";
    public static final String MODE_KEY = "_mode_";
    public static final int NOTIFICATION = 10002;
    public static boolean NOTIFICATION_TRACKER;
    private static int setState = 0;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Locations locations = new Locations(this);
    private final ArrayList<HashMap<String, String>> items = new ArrayList<>();
    private final ArrayList<Set<Integer>> storedDays = new ArrayList<>();
    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {

    }

    /**]
     * @see android.app.Service onStartCommand
     *
     * Sets up permissions and sets device volume to normal. Reads the DB and adds all the data to a hashset to be used later
     * without needing to call the SQL DB again.
     *
     * @param intent
     * @param flags
     * @param startID
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        // upon restart remove notification if it is activated - once removed the system should revert the phone back to intended settings. If not, it'll pause again.
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        resetNotification();
        locations.openReadOnlyDB();
        Cursor cursor = locations.getAllItems();
        final AudioManager myAudioManager;
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //setting the state of the app to be normal.
        setState = AudioManager.RINGER_MODE_NORMAL;
        // setting the state to normal - may not be successful if user has changed mode using hardware. State is still NORMAL therefore system will detect if change was not successful and pause.
        myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        // adding items from the database to the local hashset -----------------------------
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            HashMap<String, String> item = new HashMap<>();
            item.put(WIFI_NAME_KEY, cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
            String start_Time = String.valueOf(TimeHandling.dismantleFancyTime(cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD)))); //change the time from e..g 07:00 to 7
            item.put(START_TIME_KEY, start_Time);
            String mode = cursor.getString(cursor.getColumnIndex(Locations.MODE_FIELD));
            if (mode.equals("Vibrate")) { //didn't use boolean due to readability and changed to int for system reasons as per code below.
                mode = "1";
            } else {
                mode = "0";
            }
            item.put(MODE_KEY, mode);
            String end_Time = String.valueOf(TimeHandling.dismantleFancyTime(cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD)))); //change the time from e..g 07:00 to 7
            item.put(END_TIME_KEY, end_Time);
            String tempdays = cursor.getString(cursor.getColumnIndex(Locations.DAYS_OF_WEEK_FIELD));
            String[] days = tempdays.split(",");
            Set<Integer> daySet = new HashSet<>(); //create set of days
            for (String day : days) {
                daySet.add(Integer.parseInt(day));
            }
            storedDays.add(daySet);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();
        locations.closeDB();
        //---------------------------------------------------------------------------------------
        //fires off the looper to check conditions every 1 minute.
        looper();
        return START_STICKY;
    }

    /**
     * Removes notification from device screen.
     */
    private void resetNotification() {

        // check if notification is active prior to trying to cancel to avoid NPE.
        if (NOTIFICATION_TRACKER) {
            mNotificationManager.cancel(NOTIFICATION);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopSelf();
        executorService.shutdownNow();
        super.onDestroy();

    }

    /**
     * Schedules check method to be run every 1 minute
     */
    private void looper() {
        executorService.scheduleAtFixedRate(this::check, 0, 1, TimeUnit.MINUTES);
    }

    /**
     * Runs through every condition retrieved from the DB and checks if the current system settings match and if so carries out the required actions.
     *
     * If the user changes the volume of the device (silent/vibrate/normal) check will not make changes until that change is reversed and will call @changeNotification
     */
    private void check() {
        final AudioManager myAudioManager;
        int mode = 0;
        // get connected SSID and AUDIO Settings -------
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
        String wifiName;
        Boolean changeSettings = false;


        // check that the wifi is connected
        if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            wifiName = wifiInfo.getSSID();
        } else {
            wifiName = "";
        }

        wifiName = wifiName.replace("\"", ""); //remove "" from Wifi network.
        //-------------------------------
        //get time from system
        Calendar c = Calendar.getInstance();
        int hournow = c.get(Calendar.HOUR_OF_DAY);
        int minutenow = c.get(Calendar.MINUTE);
        int hour = (hournow * 100) + minutenow; // multiply hour by 100 so minutes can be added, i.e. 7x 100 = 700 + 10 minutes = 710.
        int today = c.get(Calendar.DAY_OF_WEEK);


        if (setState == myAudioManager.getRingerMode()) { //checking if the state is actually what we set it to last (i.e has the user changed it manually?).
            resetNotification(); // remove notification if so - avoiding manual reset.
            // checking all conditions in hash set against the system conditions to see if they meet.
            for (int i = 0; i < items.size(); i++) {

                // retrieve individual values from the hashset ---
                String wifiNetworkName = items.get(i).get(WIFI_NAME_KEY);
                int start_Time = Integer.parseInt(items.get(i).get(START_TIME_KEY));
                int end_Time = Integer.parseInt(items.get(i).get(END_TIME_KEY));
                Set<Integer> daySetRetrieved = storedDays.get(i);
                // ------------------------------------------------


                if (start_Time < end_Time && Objects.equals(wifiNetworkName, wifiName) && daySetRetrieved.contains(today)) { // if start time < end time, then this is not an overnight condition
                    if (start_Time <= hour && hour < end_Time) { // time must within the two ranges if it's all on the same day.
                        changeSettings = true;
                        mode = Integer.parseInt(items.get(i).get(MODE_KEY));
                    }
                } else if (hour >= start_Time && Objects.equals(wifiNetworkName, wifiName)) { //if the time is bigger than the end time, it MUST span 2 days. therefore if time is bigger then start time and it's still today, we're good.
                    if (daySetRetrieved.contains(today)) {
                        changeSettings = true;
                        mode = Integer.parseInt(items.get(i).get(MODE_KEY));
                    }
                } else if (hour < end_Time && Objects.equals(wifiNetworkName, wifiName)) { //if the time is not bigger than the start time, we must be in the new day. Now check if the endtime is smaller than the current hour and day is the next day.
                    if (daySetRetrieved.contains(today - 1) || (today == 1 && daySetRetrieved.contains(7))) {
                        changeSettings = true;
                        mode = Integer.parseInt(items.get(i).get(MODE_KEY));
                    }
                }
            }

            //if we met a condition we activate this setting and change the mode to the last known mode setting that was matched.
            if (changeSettings) {
                if (myAudioManager.getRingerMode() != mode) {
                    myAudioManager.setRingerMode(mode);
                    setState = myAudioManager.getRingerMode(); //if user has manually changed, we may not be able to change, therefore, we set the app state to what we want it to be and check if it matches later.
                }
            // if no conditions are met, then we revert the phone back to Normal.
            } else if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                setState = AudioManager.RINGER_MODE_NORMAL;//if user has manually changed, we may not be able to change, therefore, we set the app state to what we want it to be and check if it matches later.
            }
        } else {
            changeNotification(); //if states didn't match at the top, then this method is called to display the notification.
        }
    }

    /**
     * Notification that is sent to the screen if the user changes the setting set by the app. For example, if the user sets the phone to silent manually the app
     * will pause any changes, rather than fight the user to change it back to normal.
     */
    private void changeNotification() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("Lotisi Automatic Service Stopped")
                        .setContentText("Click Reset in app/revert any hardware sliders to normal")
                        .setContentIntent(resultPendingIntent);

        PendingIntent.getActivity(this, 0, new Intent(this, AutoSilenceService.class), 0);
        // notificationID  update of the notification later on.
        mNotificationManager.notify(NOTIFICATION, mBuilder.build());
        NOTIFICATION_TRACKER = true;


    }
}
