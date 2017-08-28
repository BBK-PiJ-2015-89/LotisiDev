package testapp.silencertestapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static final String WIFI_NAME_KEY = "_wifi_name";
    public static final String START_TIME_KEY = "_start_time";
    public static final String END_TIME_KEY = "_end_time";
    private EditText wifiName;
    private EditText start;
    private EditText end;
    private ListView itemList;
    private Button add;
    private CheckBox monday;
    private CheckBox tuesday;
    private CheckBox wednesday;
    private CheckBox thursday;
    private CheckBox friday;
    private CheckBox saturday;
    private CheckBox sunday;

    private long selectedItem = -1;
    private final Locations locations = new Locations(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locations.openWriteDB();
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
        add = (Button) findViewById(R.id.add);
        monday = (CheckBox) findViewById(R.id.monCheckbox);
        tuesday = (CheckBox) findViewById(R.id.tuCheckbox);
        wednesday = (CheckBox) findViewById(R.id.wedCheckBox);
        thursday = (CheckBox) findViewById(R.id.thuCheckbox);
        friday = (CheckBox) findViewById(R.id.friCheckbox);
        saturday = (CheckBox) findViewById(R.id.satCheckbox);
        sunday = (CheckBox) findViewById(R.id.sunCheckbox);




        reloadAdapter();


        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                untickCheckBoxes();
                Cursor cursor = locations.getConditionByID(l);
                cursor.moveToNext();
                wifiName.setText(cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
                start.setText(cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD)));
                end.setText(cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD)));
                String tempdays = cursor.getString(cursor.getColumnIndex(Locations.DAYS_OF_WEEK_FIELD));
                String[] days = tempdays.split(",");
                Set<Integer> daySet = new HashSet<>();
                for (String day : days) {
                    daySet.add(Integer.parseInt(day));
                }
                if(daySet.contains(1)){
                    sunday.setChecked(true);
                }if(daySet.contains(2)){
                    monday.setChecked(true);
                }if(daySet.contains(3)){
                    tuesday.setChecked(true);
                }if(daySet.contains(4)){
                    wednesday.setChecked(true);
                }if(daySet.contains(5)){
                    thursday.setChecked(true);
                }if(daySet.contains(6)){
                    friday.setChecked(true);
                }if(daySet.contains(7)){
                    saturday.setChecked(true);
                }
                add.setText("Edit");
                cursor.close();
                selectedItem = l;
            }
        });

        itemList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {


            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                displayDialog(l);
                return true;
            }
        });
    }

    public void add(View view) {
        if (!wifiName.getText().toString().equals("") && (monday.isChecked() || tuesday.isChecked() || wednesday.isChecked() || thursday.isChecked() || friday.isChecked() || saturday.isChecked() || sunday.isChecked()) && !start.getText().toString().equals("") && !end.getText().toString().equals("")) {

            Locations locations = new Locations(this);
            locations.openWriteDB();
            Set<Integer> days = new HashSet<>();

            //checking which days are selected before edit/write -------
            if(monday.isChecked()){
                days.add(2);
            }
            if (tuesday.isChecked()){
                days.add(3);
            }
            if(wednesday.isChecked()){
                days.add(4);
            }
            if(thursday.isChecked()){
                days.add(5);
            }
            if(friday.isChecked()){
                days.add(6);
            }
            if(saturday.isChecked()){
                days.add(7);
            }
            if(sunday.isChecked()){
                days.add(1);
            }
            //-----------------------------------------------------------


            //store days array string result in friendly way
            String stringDays = days.toString();
            stringDays = stringDays.replace("[", "");
            stringDays = stringDays.replace("]", "");
            stringDays = stringDays.replace(" ", "");

            //------

            if(selectedItem == -1){
                locations.addCondition(wifiName.getText().toString(), Integer.parseInt(start.getText().toString()), Integer.parseInt(end.getText().toString()), stringDays);

            }
            else{
                locations.updateConditionById(selectedItem, wifiName.getText().toString(), Integer.parseInt(start.getText().toString()), Integer.parseInt(end.getText().toString()), stringDays);
                selectedItem = -1;
                add.setText("Add");
                Toast.makeText(this, "Successfully edited", Toast.LENGTH_SHORT).show();
            }

            locations.closeDB();
            reloadAdapter();

            //restart service due to update in DB --------
            Intent intent = new Intent(this, AutoSilenceService.class);
            stopService(intent);
            startService(intent);
            //---------------------

        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Alert");
            builder.setMessage("All fields are mandatory and you must select at least one day.");
            builder.setCancelable(true);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    private void reloadAdapter() {


        Cursor cursor = locations.getAllItems();

        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,R.layout.listitem,cursor,
                new String[]{Locations.WIFI_NAME_FIELD, Locations.START_TIME_FIELD, Locations.END_TIME_FIELD},
                new int[]{R.id.wifi_network_listview, R.id.start_listview, R.id.end_time_listview},1);

        itemList.setAdapter(simpleCursorAdapter);

        wifiName.setText("");
        start.setText("");
        end.setText("");
        untickCheckBoxes();
    }

    private void untickCheckBoxes() {
        monday.setChecked(false);
        tuesday.setChecked(false);
        wednesday.setChecked(false);
        thursday.setChecked(false);
        friday.setChecked(false);
        saturday.setChecked(false);
        sunday.setChecked(false);
    }

    private void displayDialog(final long selected){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert");
        builder.setMessage("Do you really want to remove this condition?");
        Intent intent = new Intent(this, AutoSilenceService.class);
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //delete

                locations.removeConditionById(selected);
                reloadAdapter();
                Toast.makeText(MainActivity.this, "Removed Condition", Toast.LENGTH_SHORT).show();
                stopService(intent);
                startService(intent);
            }
        });

        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

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
        int today = c.get(Calendar.DAY_OF_WEEK);

        ArrayList<HashMap<String, String>> items = new ArrayList<>();
        ArrayList<Set<Integer>> storedDays = new ArrayList<>();




        Cursor cursor = locations.getAllItems();
        cursor.moveToFirst();
        for (int i = 0; i <cursor.getCount() ; i++) {
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

            for (int i = 0; i < items.size(); i++) {

                String wifiNetworkName = items.get(i).get(WIFI_NAME_KEY);
                int start_Time = Integer.parseInt(items.get(i).get(START_TIME_KEY));
                int end_Time = Integer.parseInt(items.get(i).get(END_TIME_KEY));
                Set<Integer> daySetRetrieved = storedDays.get(i);

                if (start_Time < end_Time && Objects.equals(wifiNetworkName, name) && daySetRetrieved.contains(today)) {
                    if (start_Time <= hour && hour < end_Time) {
                        setToSilent = true;
                    }
                } else if (hour >= start_Time && Objects.equals(wifiNetworkName, name)) {
                    if(daySetRetrieved.contains(today)){
                    setToSilent = true;}
                } else if (hour <= end_Time && Objects.equals(wifiNetworkName, name) && daySetRetrieved.contains(today-1)) {
                    setToSilent = true;
                } else {
                    //do nothing
                }

        if (setToSilent) {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            Toast.makeText(getApplicationContext(), "Silenced", Toast.LENGTH_SHORT).show();
        } else if (myAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            Toast.makeText(getApplicationContext(), "Reverted back to original normal volume", Toast.LENGTH_SHORT).show();
        }

    }
}}