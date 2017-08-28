package testapp.silencertestapp;

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
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
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

    //private ScheduledFuture future;
    public static final String START_TIME_KEY = "_start_time";
    public static final String END_TIME_KEY = "_end_time";
    static int setState = 250;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Locations locations = new Locations(this);
    private final ArrayList<HashMap<String, String>> items = new ArrayList<>();
    private final ArrayList<Set<Integer>> storedDays = new ArrayList<>();

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        System.out.println("starting service");
        locations.openReadOnlyDB();
        Cursor cursor = locations.getAllItems();

        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            HashMap<String, String> item = new HashMap<>();
            item.put(WIFI_NAME_KEY, cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
            item.put(START_TIME_KEY, cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD)));
            item.put(END_TIME_KEY, cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD)));
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


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "Auto Silence Stopped", Toast.LENGTH_LONG).show();
        stopSelf();
        //future.cancel(true);
        executorService.shutdownNow();
        //System.exit(1);
        super.onDestroy();

    }

    private void looper() {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                check();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void check() {
        final AudioManager myAudioManager;
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
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int today = c.get(Calendar.DAY_OF_WEEK);

        System.out.println("running service");

        if (setState == 250) {
            setState = myAudioManager.getRingerMode();
        }
        if (setState == myAudioManager.getRingerMode()) {
            for (int i = 0; i < items.size(); i++) {

                String wifiNetworkName = items.get(i).get(WIFI_NAME_KEY);
                int start_Time = Integer.parseInt(items.get(i).get(START_TIME_KEY));
                int end_Time = Integer.parseInt(items.get(i).get(END_TIME_KEY));
                Set<Integer> daySetRetrieved = storedDays.get(i);

                if (start_Time < end_Time && Objects.equals(wifiNetworkName, wifiName) && daySetRetrieved.contains(today)) {
                    if (start_Time <= hour && hour < end_Time) {
                        setToSilent = true;
                    }
                } else if (hour >= start_Time && Objects.equals(wifiNetworkName, wifiName)) {
                    if(daySetRetrieved.contains(today)){
                        setToSilent = true;}
                } else if (hour <= end_Time && Objects.equals(wifiNetworkName, wifiName) && daySetRetrieved.contains(today-1)) {
                    setToSilent = true;
                } else {
                    //do nothing
                }
            }
            if (setToSilent) {
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                setState = myAudioManager.getRingerMode();

            } else if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                setState = myAudioManager.getRingerMode();
            }
        } else {
            System.out.println("changed from set setting");
        }
    }

}
