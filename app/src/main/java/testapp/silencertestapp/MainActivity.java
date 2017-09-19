package testapp.silencertestapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //private EditText wifiName;
    private TimePicker start;
    private TimePicker end;
    private ListView itemList;
    private Button add;
    private CheckBox monday;
    private CheckBox tuesday;
    private CheckBox wednesday;
    private CheckBox thursday;
    private CheckBox friday;
    private CheckBox saturday;
    private CheckBox sunday;
    private Spinner spinner;
    private String DEFAULT_WIFI_TEXT = "Select WiFi Network";


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

        spinner = (Spinner) findViewById(R.id.wiFiSpinner);
        //wifiName = (EditText) findViewById(R.id.wifinetwork);
        start = (TimePicker) findViewById(R.id.startPicker);
        end = (TimePicker) findViewById(R.id.endPicker);
        itemList = (ListView) findViewById(R.id.item_List);
        add = (Button) findViewById(R.id.add);
        monday = (CheckBox) findViewById(R.id.monCheckbox);
        tuesday = (CheckBox) findViewById(R.id.tuCheckbox);
        wednesday = (CheckBox) findViewById(R.id.wedCheckBox);
        thursday = (CheckBox) findViewById(R.id.thuCheckbox);
        friday = (CheckBox) findViewById(R.id.friCheckbox);
        saturday = (CheckBox) findViewById(R.id.satCheckbox);
        sunday = (CheckBox) findViewById(R.id.sunCheckbox);

        start.setIs24HourView(true);
        end.setIs24HourView(true);

        setSpinner(getSSID(DEFAULT_WIFI_TEXT));


        reloadAdapter();


        itemList.setOnItemClickListener((adapterView, view, i, l) -> {
            untickCheckBoxes();
            Cursor cursor = locations.getConditionByID(l);
            cursor.moveToNext();

            String startHourCombined = cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD));
            String endHourCombined = cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD));
            System.out.println(startHourCombined);
            System.out.println(endHourCombined);

            int startTimeIntHour = getHour(startHourCombined);
            int startTimeIntMinute = getMinute(startHourCombined);
            int endTimeIntHour = getHour(endHourCombined);
            int endTimeIntMinute = getMinute(endHourCombined);

            String[] wifiNames = getSSID(cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
            setSpinner(wifiNames);
            //wifiName.setText(cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
            start.setHour(startTimeIntHour);
            start.setMinute(startTimeIntMinute);
            end.setHour(endTimeIntHour);
            end.setMinute(endTimeIntMinute);
            //start.setText(cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD)));
            //end.setText(cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD)));
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
        });

        itemList.setOnItemLongClickListener((adapterView, view, i, l) -> {
            displayDialog(l);
            return true;
        });
    }

    private void setSpinner(String[] ssidArray) {
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ssidArray));
    }

    @NonNull
    private String[] getSSID(String extra) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks =  wifiManager.getConfiguredNetworks();
        if(configuredNetworks!= null){
        String[] ssidArray = new String[configuredNetworks.size()];
        for (int i = 0; i <configuredNetworks.size() ; i++) {
            String temp = configuredNetworks.get(i).SSID;
            temp = temp.replace("\"", "");
            ssidArray[i] = temp;
        }
        Arrays.sort(ssidArray, 1, ssidArray.length, String.CASE_INSENSITIVE_ORDER);
        String[] copiedArray = new String[ssidArray.length+1];
        System.arraycopy(ssidArray, 0, copiedArray, 1, copiedArray.length - 1);
        copiedArray[0] = extra;
        return copiedArray;}
        else{
            String[] failedArray = new String[2];
            failedArray[0] = extra;
            failedArray[1] = "WIFI IS TURNED OFF";
            return failedArray;
        }
    }

    private int getHour(String hourCombined) {
        hourCombined = Integer.toString(dismantleFancyTime(hourCombined));
        int intHour;
        if((hourCombined.length()==4)){
            intHour = Integer.parseInt(hourCombined.substring(0, 2));

        }
        else if (hourCombined.length()==3){
            intHour = Integer.parseInt(hourCombined.substring(0, 1));

        }
        else if (hourCombined.length()==2){
            intHour = 0;

        }
        else{
            intHour = 0;

        }
        return intHour;
    }

    private int getMinute(String minuteCombined) {
        minuteCombined = Integer.toString(dismantleFancyTime(minuteCombined));
        int IntMinute;
        if(minuteCombined.length()==4){
            System.out.println(minuteCombined  + " this is the end minute combined");
            IntMinute = Integer.parseInt(minuteCombined.substring(2,4));
        }
        else if (minuteCombined.length()==3){

            IntMinute = Integer.parseInt(minuteCombined.substring(1,3));
        }
        else if (minuteCombined.length()==2){

            IntMinute = Integer.parseInt(minuteCombined.substring(0,2));
        }
        else{

            IntMinute = Integer.parseInt(minuteCombined);
        }
        return IntMinute;
    }

    public void add(View view) {
        if (!Objects.equals(spinner.getSelectedItem().toString(), DEFAULT_WIFI_TEXT) && (monday.isChecked() || tuesday.isChecked() || wednesday.isChecked() || thursday.isChecked() || friday.isChecked() || saturday.isChecked() || sunday.isChecked()) && ((start.getHour()*100 + start.getMinute() != (end.getHour()*100 + end.getMinute())))) {

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


            int startHour = start.getHour();
            int startMinute = start.getMinute();
            int combinedStartTime = (startHour *100) + startMinute;

            int endHour = end.getHour();
            int endMinute = end.getMinute();
            int combinedEndTime = (endHour * 100) + endMinute;
            String combinedFancyEndTime = createFancyTime(combinedEndTime);
            String combinedFancyStartTime = createFancyTime(combinedStartTime);


            if(selectedItem == -1){
                locations.addCondition(spinner.getSelectedItem().toString(), combinedFancyStartTime, combinedFancyEndTime, stringDays);
            }
            else{
                locations.updateConditionById(selectedItem, spinner.getSelectedItem().toString(), combinedFancyStartTime, combinedFancyEndTime, stringDays);
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
            builder.setMessage("Ensure you have entered a Wifi Network and applied the rule to at least 1 day. You must also ensure your start and end time are not identical.");
            builder.setCancelable(true);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }


    private int dismantleFancyTime(String combinedTime){
        return Integer.parseInt(combinedTime.replace(":", ""));
    }

    private String createFancyTime(int combinedTime) {

        StringBuilder tempString = new StringBuilder(Integer.toString(combinedTime));
        if(tempString.length()==4){
            tempString = tempString.insert(2, ":");
        }
        else if (tempString.length()==3){
            tempString = tempString.insert(1, ":");
            tempString = tempString.insert(0, "0");
        }
        else if(tempString.length()==2){
            tempString = tempString.insert(0, "00:");
        }
        else if(tempString.length()==1){
            tempString = tempString.insert(0, "00:0");
        }
        return tempString.toString();
    }

    private void reloadAdapter() {


        Cursor cursor = locations.getAllItems();

        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,R.layout.listitem,cursor,
                new String[]{Locations.WIFI_NAME_FIELD, Locations.START_TIME_FIELD, Locations.END_TIME_FIELD},
                new int[]{R.id.wifi_network_listview, R.id.start_listview, R.id.end_time_listview},1);

        itemList.setAdapter(simpleCursorAdapter);

        //wifiName.setText("");
        start.setHour(0);
        start.setMinute(0);
        end.setHour(0);
        end.setMinute(0);
        untickCheckBoxes();
        setSpinner(getSSID(DEFAULT_WIFI_TEXT));
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

        builder.setNegativeButton("cancel", (dialogInterface, i) -> {

        });

        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

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
    }