package testapp.silencertestapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Main Activity is the base class for the Lotisi app, it runs the UI that the user interacts with.
 * The class interacts with a background service: AutoSilenceSerive and a SQL Database: Locations.
 *
 * Android widgets are defined within the top level and defined within @onCreate method so they are
 * available in all areas of the class.
 *
 * @author graemewilkinson
 * @version 1.5
 * @since 0.9
 */
public class MainActivity extends AppCompatActivity {

    //need access across app to GUI items, so they are specified here.
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
    private ToggleButton mode_button;


    private long selectedItem = -1;
    private final Locations locations = new Locations(this);

    /**
     * On creation all of the UI widgets are assigned to the values specified in the top level of the class.
     *
     * The database is opened and the GUI is specified. The app checks if it has permissions over the Do Not Disturb
     * features of the Android phone and if not requests the user to give permissions.
     *
     * The onclick listener monitors the ListView of the conditions and when the user clicks the entries they are
     * either removed or brought up for editing.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //open DB connection ready for editing.
        locations.openWriteDB();
        setContentView(R.layout.activity_main);

        //testing if we have permission to Do Not Disturb and if not, user is requested to add it.
        //taken from https://stackoverflow.com/questions/39151453/in-android-7-api-level-24-my-app-is-not-allowed-to-mute-phone-set-ringer-mode/39152607

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            Toast.makeText(this, "Provide Lotisi with Do Not Disturb access please, then press back to return.", Toast.LENGTH_LONG).show();
            startActivity(intent);
        }

        //initialise android widgets
        spinner = (Spinner) findViewById(R.id.wiFiSpinner);
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
        mode_button = (ToggleButton) findViewById(R.id.mode_button);


        start.setIs24HourView(true);
        end.setIs24HourView(true);

        setSpinner(getSSID(DEFAULT_WIFI_TEXT)); // initialise spinner data.

        reloadAdapter();

        // user clicks once and does not hold. Loads condition in to GUI screen so it can be edited.
        itemList.setOnItemClickListener((adapterView, view, i, l) -> {
            tickCheckBoxes(false);//reset all the day boxes which may have been ticked by the user.
            Cursor cursor = locations.getConditionByID(l); // retrieve the selected condition.
            cursor.moveToNext();

            String startHourCombined = cursor.getString(cursor.getColumnIndex(Locations.START_TIME_FIELD));
            String endHourCombined = cursor.getString(cursor.getColumnIndex(Locations.END_TIME_FIELD));


            int startTimeIntHour = TimeHandling.getHour(startHourCombined);
            int startTimeIntMinute = TimeHandling.getMinute(startHourCombined);
            int endTimeIntHour = TimeHandling.getHour(endHourCombined);
            int endTimeIntMinute = TimeHandling.getMinute(endHourCombined);
            String mode = cursor.getString(cursor.getColumnIndex(Locations.MODE_FIELD));
            String[] wifiNames = getSSID(cursor.getString(cursor.getColumnIndex(Locations.WIFI_NAME_FIELD)));
            setSpinner(wifiNames); // set spinner with this WiFi name included as the extra to ensure it's selectable.

            //set the GUI to show condition data ----
            start.setHour(startTimeIntHour);
            start.setMinute(startTimeIntMinute);
            end.setHour(endTimeIntHour);
            end.setMinute(endTimeIntMinute);

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

            if(mode.equals("Vibrate")){
                mode_button.setChecked(true);
            }
            else{
                mode_button.setChecked(false);
            }
            //----------------------------------------------------------------------------------------
            add.setText("Edit"); // change button text to edit so it's clear what is happening for the user.
            cursor.close();
            selectedItem = l;
        });

        //user clicks and holds on item in condition list - delete item
        itemList.setOnItemLongClickListener((adapterView, view, i, l) -> {
            displayDialog(l);
            return true;
        });
    }

    /**
     * The spinner is used to allow the user to select from a list of Saved Networks
     * in the app, rather than manually type.
     *
     * @param ssidArray String Array of network names
     */
    private void setSpinner(String[] ssidArray) {
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ssidArray));
    }

    /**
     * resetSystem restarts the background service and calls reloadAdapter.
     *
     * * @param view
     */
    public void resetSystem(View view) {
        startService(view);
        reloadAdapter();
    }

    /**
     * Returns a String Array of SSID's plus an additional value passed in as the first value in the returning list.
     * The Wifi network strings are called from the Android System and then stripped of their "" and then added to a new list,
     * ordered (not case sensitive), an extra value is added at the front (either the network name being edited, or "Please Select
     * Wifi Network" and returned for future use, for example for the spinner.
     *
     * If the Wifi is off on the connected device, then null will be returned, if this is the case, the extra is added to the list and
     * an error message added to the end. An Array is then returned with these two values.
     *
     * @param extra additional value passed in as first value in returning list.
     * @return list of Wifi networks plus additional extra value at the front.
     */
    @NonNull
    @VisibleForTesting
    public String[] getSSID(String extra) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks =  wifiManager.getConfiguredNetworks(); // retrieve configured networks
        if(configuredNetworks!= null){
            String[] ssidArray = new String[configuredNetworks.size()]; // new list to place SSID's.
                for (int i = 0; i <configuredNetworks.size() ; i++) {
                    String temp = configuredNetworks.get(i).SSID;
                    temp = temp.replace("\"", "");
                    ssidArray[i] = temp;
                }
            Arrays.sort(ssidArray, 1, ssidArray.length, String.CASE_INSENSITIVE_ORDER);
            String[] copiedArray = new String[ssidArray.length+1]; // new list one longer than the SSID list to ensure we can add a value to the front, the extra.
            System.arraycopy(ssidArray, 0, copiedArray, 1, copiedArray.length - 1);
            copiedArray[0] = extra;
            return copiedArray;
        }
        // if wifi is off, then configured network call will return null - we still need to have the extra, so we add the extra to the front and then add an error message next to it.
        else if(extra.equals(DEFAULT_WIFI_TEXT)) {
            String[] failedArray = new String[1];
            failedArray[0] = "TURN WIFI ON";
            return failedArray;
        }
        else{
            String[] failedArray = new String[2];
            failedArray[0] = extra;
            failedArray[1] = "WIFI IS TURNED OFF";
            return failedArray;
        }
    }

    /**
     * Adds or edits an entry in the database. The UI is used to pull the information entered by the user.
     * methods are used to obtain the times from the stored database and break then down to data that can be read and compared
     * by the code and vice versa.
     *
     * When an action is successfully carried out a relevant Toast is shown confirming that is the case.
     * @param view
     */
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
            String combinedFancyEndTime = TimeHandling.createFancyTime(combinedEndTime);
            String combinedFancyStartTime = TimeHandling.createFancyTime(combinedStartTime);
            String mode = "Silence";
            boolean mode_setting = mode_button.isChecked();
            if(mode_setting){
                mode = "Vibrate";
            }

            // if -1 we are adding a new condition, if 1 we are editing an old one.
            if(selectedItem == -1){
                locations.addCondition(spinner.getSelectedItem().toString(), mode, combinedFancyStartTime, combinedFancyEndTime, stringDays);
            }
            else{
                locations.updateConditionById(selectedItem, spinner.getSelectedItem().toString(), mode, combinedFancyStartTime, combinedFancyEndTime, stringDays);
                selectedItem = -1;
                add.setText("Add"); //revert back to 'add' text on button as we've just edited, which means it was previously 'edit'.
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
            builder.setTitle("Further Details Needed");
            builder.setMessage("Ensure you have selected a Wifi Network and applied the rule to at least 1 day.\n\n You must also ensure your start and end time are not identical.\n\n Do not create overlapping duplicate WIFI conditions.");
            builder.setCancelable(true);
            builder.setNegativeButton("OK", (dialogInterface, i) -> {

            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    /**
     * A pop up within the UI that provides some basic help - two buttons are available OK and Furher Information -
     * further information links to an external site.
     * @param view
     */
    public void helpSection(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What does this app do?");
        builder.setMessage("Lotisi is designed to put your phone on silent/vibrate automatically at certain times\n\n Step 1: Enable Do Not Disturb Access on start up\n\n\n Step 2: Select WiFi Network\n\n Step 3: Select Start and End times or click 'Always'\n\n Step 3: Select days you want to run if you didn't click always\n\n Step 5: Select the mode, vibrate or silent\n\n Step 6: Select Add\n\n\n If you wish to edit a condition, select it from the list \n\n If you wish to delete a condition, select and hold it. \n\n To start the app click reset");
        builder.setCancelable(true);
        builder.setPositiveButton("Further Help", (dialogInterface, i) -> {
            //go to external page - not currently set up so just google.com
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
            startActivity(browserIntent);

        });
        builder.setNegativeButton("cancel", (dialogInterface, i) -> {

        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * reloadAdapter is called to reset the UI after a condition is added or edited. The method resets all values back to their original settings
     * and updates the ListView with the latest values from the database which have probably just been changed.
     */
    private void reloadAdapter() {
        Cursor cursor = locations.getAllItems();

        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this,R.layout.listitem,cursor,
                new String[]{Locations.WIFI_NAME_FIELD, Locations.MODE_FIELD, Locations.START_TIME_FIELD, Locations.END_TIME_FIELD},
                new int[]{R.id.wifi_network_listview, R.id.mode_listview, R.id.start_listview, R.id.end_time_listview},1);

        itemList.setAdapter(simpleCursorAdapter);
        start.setHour(0);
        start.setMinute(0);
        end.setHour(0);
        end.setMinute(0);
        tickCheckBoxes(false);
        setSpinner(getSSID(DEFAULT_WIFI_TEXT));
        mode_button.setChecked(false);
    }

    /**
     * Sets the day of the week tick boxes in the UI to on or off
     * @param onOrOff TRUE: ticked FALSE: unticked
     */
    void tickCheckBoxes(boolean onOrOff) {
        monday.setChecked(onOrOff);
        tuesday.setChecked(onOrOff);
        wednesday.setChecked(onOrOff);
        thursday.setChecked(onOrOff);
        friday.setChecked(onOrOff);
        saturday.setChecked(onOrOff);
        sunday.setChecked(onOrOff);
    }

    /**
     * Changes the settings in the UI to ensure that the condition is always met in relation to time of day and day of the week.
     *
     * @param view
     */
    public void alwaysOn(View view){
        //due to time being always on and all days selected, when connected to the particular wifi the condition will always be met.
        end.setHour(23);
        end.setMinute(59);
        start.setMinute(0);
        start.setHour(0);
        tickCheckBoxes(true);
    }

    /**
     * This dialogue works as a warning to ask the user if the intention is really to delete
     * the database entry they have clicked on.
     * @param selected database entry ID.
     */
    void displayDialog(final long selected){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert");
        builder.setMessage("Do you really want to remove this condition?");
        Intent intent = new Intent(this, AutoSilenceService.class);
        builder.setPositiveButton("Remove", (dialogInterface, i) -> {
            //delete

            locations.removeConditionById(selected);
            reloadAdapter();
            add.setText("Add");
            selectedItem = -1;
            Toast.makeText(MainActivity.this, "Removed Condition", Toast.LENGTH_SHORT).show();
            stopService(intent);
            startService(intent);
        });

        builder.setNegativeButton("cancel", (dialogInterface, i) -> {

        });

        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    /**
     * Start the background service, calling again will restart.
     * @param view
     */
    public void startService(View view) {

        Intent intent = new Intent(this, AutoSilenceService.class);
        startService(intent);
        Toast.makeText(this, "Auto Silence Service Started... ", Toast.LENGTH_LONG).show();
    }

}