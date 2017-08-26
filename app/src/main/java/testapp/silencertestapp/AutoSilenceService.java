package testapp.silencertestapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Objects;
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
    static int setState = 250;

    //private ScheduledFuture future;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
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

    private void looper(){
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                check();
            }
        }, 0, 1, TimeUnit.MINUTES);

        /*
        Boolean loopForever = true;
        do {
            check();
            System.out.println("run");
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while(loopForever);*/


    }

    private void check(){
        final AudioManager myAudioManager;
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
        String wifiName;
        if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            wifiName = wifiInfo.getSSID();
        }
        else{
            wifiName = "";
        }

        wifiName = wifiName.replace("\"", ""); //remove ""

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);

        if(setState == 250){
            setState = myAudioManager.getRingerMode();
        }
        System.out.println("testprint");
        if(setState==myAudioManager.getRingerMode()) {
            if (Objects.equals(wifiName, "HAVELOCKHOUSE_5G") && (hour >= 23 || hour < 7)) {
                if(myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                    myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    System.out.println("silence");
                    setState = AudioManager.RINGER_MODE_SILENT;
                }
            } else if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                System.out.println("revert");
                setState = AudioManager.RINGER_MODE_NORMAL;
            }
        }
        else{
            System.out.println("state changed since SilenceApp updated - revert back for app to take back control");
        }
    }



  /*  private void autoSilenceLoop() {

        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(checker::checker, 0, 1, TimeUnit.MINUTES);


    private void checker() {


    }
}*/
    }
