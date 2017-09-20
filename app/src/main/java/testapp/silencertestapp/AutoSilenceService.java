package testapp.silencertestapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by graemewilkinson on 01/08/2017.
 */

public class AutoSilenceService extends Service {
    public static final String WIFI_NAME_KEY = "_wifi_name";
    public static final String START_TIME_KEY = "_start_time";
    public static final String END_TIME_KEY = "_end_time";
    public static final String MODE_KEY = "_mode_";
    private static int setState = 0;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Locations locations = new Locations(this);
    private final ArrayList<HashMap<String, String>> items = new ArrayList<>();
    private final ArrayList<Set<Integer>> storedDays = new ArrayList<>();
    private NotificationManager mNotificationManager;
    public static final int NOTIFICATION = 10002;
    public static boolean NOTIFICATION_TRACKER;

    @Override
    public void onCreate() {

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        mNotificationManager=  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(NOTIFICATION_TRACKER){
            mNotificationManager.cancel(NOTIFICATION);
        }
        locations.openReadOnlyDB();
        Cursor cursor = locations.getAllItems();
        final AudioManager myAudioManager;
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setState = myAudioManager.getRingerMode();

        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            HashMap<String, String> item = new HashMap<>();
            item.put(WIFI_NAME_KEY, cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
            String start_Time = dismantleFancyTime(cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD)));
            item.put(START_TIME_KEY, start_Time);
            String mode = cursor.getString(cursor.getColumnIndex(Locations.MODE_FIELD));
            if(mode.equals("Vibrate")){
                mode ="1";
            }
            else{
                mode = "0";
            }
            item.put(MODE_KEY, mode);
            String end_Time = dismantleFancyTime(cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD)));
            item.put(END_TIME_KEY, end_Time);
            String tempdays = cursor.getString(cursor.getColumnIndex(Locations.DAYS_OF_WEEK_FIELD));
            String[] days = tempdays.split(",");
            Set<Integer> daySet = new HashSet<>();
            for (String day : days) {
                daySet.add(Integer.parseInt(day));
            }
            storedDays.add(daySet);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();
        locations.closeDB();
        looper();
        return START_STICKY;
    }
    private String dismantleFancyTime(String combinedTime){
        return combinedTime.replace(":", "");
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

    private void looper() {
        executorService.scheduleAtFixedRate(this::check, 0, 1, TimeUnit.MINUTES);
    }

    private void check() {
        final AudioManager myAudioManager;
        int mode= 0;
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
        String wifiName;
        Boolean setToSilent = false;
        if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            wifiName = wifiInfo.getSSID();
        } else {
            wifiName = "";
        }

        wifiName = wifiName.replace("\"", ""); //remove ""

        Calendar c = Calendar.getInstance();
        int hournow = c.get(Calendar.HOUR_OF_DAY);
        int minutenow = c.get(Calendar.MINUTE);
        int hour = (hournow * 100) + minutenow;
        int today = c.get(Calendar.DAY_OF_WEEK);

        if (setState == myAudioManager.getRingerMode()) {
            for (int i = 0; i < items.size(); i++) {

                String wifiNetworkName = items.get(i).get(WIFI_NAME_KEY);
                int start_Time = Integer.parseInt(items.get(i).get(START_TIME_KEY));
                int end_Time = Integer.parseInt(items.get(i).get(END_TIME_KEY));
                Set<Integer> daySetRetrieved = storedDays.get(i);
                mode = Integer.parseInt(items.get(i).get(MODE_KEY));

                if (start_Time < end_Time && Objects.equals(wifiNetworkName, wifiName) && daySetRetrieved.contains(today)) {
                    if (start_Time <= hour && hour < end_Time) {
                        setToSilent = true;
                    }
                } else if (hour >= start_Time && Objects.equals(wifiNetworkName, wifiName)) {
                    if(daySetRetrieved.contains(today)){
                        setToSilent = true;}
                } else if (hour < end_Time && Objects.equals(wifiNetworkName, wifiName)) {
                    if (daySetRetrieved.contains(today - 1) || (today == 1 && daySetRetrieved.contains(7))) {
                        setToSilent = true;
                    }
                }
            }

            if (setToSilent) {
                myAudioManager.setRingerMode(mode);
                setState = myAudioManager.getRingerMode();

            } else if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                setState = myAudioManager.getRingerMode();
            }
        } else {
            notificationExample();

        }
    }


    private void notificationExample(){
        Intent intent = new Intent(this, AutoSilenceService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("Lotisi")
                        .setContentText("Stopped due to manual change - open app and click restart")
                        .setContentIntent(pendingIntent);

                PendingIntent.getActivity(this, 0, new Intent(this, AutoSilenceService.class), 0);

        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION, mBuilder.build());
        NOTIFICATION_TRACKER = true;



    }
}
