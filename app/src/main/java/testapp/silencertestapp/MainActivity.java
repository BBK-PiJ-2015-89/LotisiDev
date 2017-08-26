package testapp.silencertestapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String WIFI_NAME_KEY = "_wifi_name";
    public static final String START_TIME_KEY = "_start_time";
    public static final String END_TIME_KEY = "_end_time";
    Button silenceBtn;
    private ArrayList<HashMap<String, String>> items = new ArrayList<>();
    private EditText wifiName;
    private EditText start;
    private EditText end;
    private ArrayAdapter<String> adapter;
    private ListView itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //testing if we have permission

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }

        //listviewtest

        wifiName = (EditText) findViewById(R.id.wifinetwork);
        start = (EditText) findViewById(R.id.start_time);
        end = (EditText) findViewById(R.id.end_time);
        itemList = (ListView) findViewById(R.id.item_List);

        //adapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, items);
        itemList.setAdapter(adapter);

        //listviewtestend
    }

    public void add(View view) {
        if (!wifiName.getText().toString().equals("")) {
            //items.add(Array[wifiName.getText().toString()][start.getText().toString()][end.getText().toString()])
            HashMap<String, String> item = new HashMap<>();
            item.put(WIFI_NAME_KEY, wifiName.getText().toString());
            item.put(START_TIME_KEY, start.getText().toString());
            item.put(END_TIME_KEY, end.getText().toString());

            items.add(item);

            //items.add(new Condition(wifiName.getText().toString(), (Integer.parseInt(start.getText().toString())), (Integer.parseInt(end.getText().toString()))));
            //items.add(wifiName.getText().toString());
            SimpleAdapter adapter = new SimpleAdapter(this, items,
                    R.layout.listitem,
                    new String[]{WIFI_NAME_KEY, START_TIME_KEY, END_TIME_KEY},
                    new int[]{R.id.wifi_network_listview, R.id.start_listview, R.id.end_time_listview});
            itemList.setAdapter(adapter);
            wifiName.setText("");
            start.setText("");
            end.setText("");

            //adapter.notifyDataSetChanged();
        }
    }

    public void changeToSilence(View view) {
        final AudioManager myAudioManager;
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String name = wifiInfo.getSSID();
        name = name.replace("\"", ""); //remove ""
        //Objects.equals(name, "HAVELOCKHOUSE_5G")

        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);


        if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            Toast.makeText(getApplicationContext(), "Silenced", Toast.LENGTH_SHORT).show();
        } else {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            Toast.makeText(getApplicationContext(), "Reverted back to original normal volume", Toast.LENGTH_SHORT).show();

        }

    }

    public void startService(View view) {

        Intent intent = new Intent(this, AutoSilenceService.class);
        startService(intent);
        Toast.makeText(this, "Auto Silence Service Started... ", Toast.LENGTH_LONG).show();
    }

    public void stopService(View view) {
        Intent intent = new Intent(this, AutoSilenceService.class);
        stopService(intent);
    }

    public void cellTowers(View view) {
        int NETWORK_TYPE_EDGE = 1; //initialized previously
        int rssi = 31;
        NeighboringCellInfo nc = new NeighboringCellInfo(rssi, "FFFFFFF", NETWORK_TYPE_EDGE);
        System.out.println(nc.getCid());


    }

    public void testMethod(View view) {

        final AudioManager myAudioManager;
        myAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String name = wifiInfo.getSSID();
        name = name.replace("\"", ""); //remove ""
        Boolean setToSilent = false;
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        for (int i = 0; i < items.size(); i++) {

            String wifiNetworkName = items.get(i).get(WIFI_NAME_KEY);
            int start_Time = Integer.parseInt(items.get(i).get(START_TIME_KEY));
            int end_Time = Integer.parseInt(items.get(i).get(END_TIME_KEY));

            if ((Objects.equals(wifiNetworkName, name) && (start_Time <= hour && end_Time > hour))){

                Toast.makeText(this, wifiNetworkName + " has met the condition TN: " + hour + " >" + start_Time + " + < " + end_Time, Toast.LENGTH_SHORT).show();
                setToSilent = true;
            } else {
                Toast.makeText(this, name + " does not equal " + wifiNetworkName, Toast.LENGTH_SHORT).show();
            }
        }

        if (setToSilent) {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            Toast.makeText(getApplicationContext(), "Silenced", Toast.LENGTH_SHORT).show();
        } else if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            Toast.makeText(getApplicationContext(), "Reverted back to original normal volume", Toast.LENGTH_SHORT).show();
        }

    }
}