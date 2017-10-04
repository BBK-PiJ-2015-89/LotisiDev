package testapp.silencertestapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database class that stores the data from the conditions set within the GUI and listed in ListVIew in MainActivity.
 *
 * Influenced by TechThree INFO . (2016, May 29). YouTube. Retrieved from SQLite Example Part 1 and 2: https://www.youtube.com/watch?v=TAio8AlsyZE

 */
public class Locations {

    /*
    <---------------- DATABASE ----->
     */

    public static final String DATABASE_NAME = "myConditions.db";
    public static final String CONDITIONS_TBL = "conditions";
    public static final String WIFI_NAME_FIELD = "_wifi_name";
    public static final String START_TIME_FIELD = "_start_time";
    public static final String END_TIME_FIELD = "_end_time";
    public static final String ITEM_ID_FIELD = "_id";
    public static final String DAYS_OF_WEEK_FIELD = "_days_of_week";
    public static final String MODE_FIELD = "_mode";
    public static final int DB_VER = 1;
    public static final String[] ALL_FIELDS = {ITEM_ID_FIELD, WIFI_NAME_FIELD, MODE_FIELD ,START_TIME_FIELD, END_TIME_FIELD, DAYS_OF_WEEK_FIELD};

    public static final String CREATE_TBL = "CREATE table "+CONDITIONS_TBL+
            " ("+ITEM_ID_FIELD+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ WIFI_NAME_FIELD+" TEXT NOT NULL, "+ MODE_FIELD+" TEXT, " +
            START_TIME_FIELD+" INTEGER NOT NULL, " +END_TIME_FIELD+" INTEGER NOT NULL, " + DAYS_OF_WEEK_FIELD+ " TEXT NOT NULL)";

    public class ConditionDBOpenHelper extends SQLiteOpenHelper{

        public ConditionDBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_VER);
        }

        /**
         * Create table in the database as per spec under CREATE_TBL String
         * @param sqLiteDatabase database object.
         */
        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TBL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            if (i<i1){
                sqLiteDatabase.execSQL("DROP table "+CONDITIONS_TBL);
                onCreate(sqLiteDatabase); //needs to retain data really
            }
        }
    }


    //----------------------
    private ConditionDBOpenHelper dpOpenHelper;
    private SQLiteDatabase db;

    public Locations(Context context){
        dpOpenHelper = new ConditionDBOpenHelper(context);

    }

    /**
     * open a read/write access to DB
     */
    public void openWriteDB(){
        db = dpOpenHelper.getWritableDatabase();
    }

    /**
     * Open a read only access to DB, used for the service to ensure that the service and UI do not conflict with access requests.
     */
    public void openReadOnlyDB(){
        db = dpOpenHelper.getReadableDatabase();
    }

    public void closeDB(){
        db.close();
        db = null;
    }

    /**
     * Retrieves all items from DB
     *
     * SQL Lite EQV of SQL SELECT* from CONDITIONS_TBL
     *
     * @return Cursor containing all items.
     */
    public Cursor getAllItems(){
        return db.query(CONDITIONS_TBL, ALL_FIELDS, null, null, null, null, null);
    }

    /**
     * Retrieves item matching item_iD from DB
     *
     * SQL Lite EQV of SQL SELECT * from CONDITIONS_TBL where ID = item_ID
     *
     * @param item_Id ID of condition in DB
     * @return Cursor containing specified condition.
     */
    public Cursor getConditionByID(long item_Id){
        Cursor cursor = db.query(CONDITIONS_TBL, ALL_FIELDS, ITEM_ID_FIELD + " = ?",
                new String[]{Long.toString(item_Id)}, null, null, null);
                return cursor;
    }

    /**
     * Adds an record to the Database
     *
     * @param wifiName String Wifi
     * @param mode  String  (vibrate/silent)
     * @param start_Time String e.g. "23:59"
     * @param end_time String e.g. "09:00"
     * @param days_of_week String, day values e.g. "1,2,3,4"
     * @return long ID of new condition.
     */
    public long addCondition(String wifiName, String mode, String start_Time, String end_time, String days_of_week){
        ContentValues contentValues = new ContentValues();
        contentValues.put(WIFI_NAME_FIELD, wifiName);
        contentValues.put(START_TIME_FIELD, start_Time);
        contentValues.put(END_TIME_FIELD, end_time);
        contentValues.put(DAYS_OF_WEEK_FIELD, days_of_week);
        contentValues.put(MODE_FIELD, mode);

       return db.insert(CONDITIONS_TBL, null, contentValues);
    }


    /**
     * Edits the specified (via itemID) record in the database.
     * @param itemId long condition ID
     * @param wifiName String Wifi
     * @param mode  String  (vibrate/silent)
     * @param start_Time String e.g. "23:59"
     * @param end_Time String e.g. "09:00"
     * @param days_of_week String, day values e.g. "1,2,3,4"
     * @return long ID of new condition.
     */
    public int updateConditionById(long itemId, String wifiName, String mode, String start_Time, String end_Time, String days_of_week){
        ContentValues contentValues = new ContentValues();
        contentValues.put(WIFI_NAME_FIELD, wifiName);
        contentValues.put(START_TIME_FIELD, start_Time);
        contentValues.put(END_TIME_FIELD, end_Time);
        contentValues.put(DAYS_OF_WEEK_FIELD, days_of_week);
        contentValues.put(MODE_FIELD, mode);

        return db.update(CONDITIONS_TBL, contentValues, ITEM_ID_FIELD+" = ?", new String[]{Long.toString(itemId)});

    }

    /**
     * Removes the specified record in the database
     *
     * @param itemId long ID of the record to be removed.
     */
    public void removeConditionById(long itemId){
        db.delete(CONDITIONS_TBL, ITEM_ID_FIELD+" = ?", new String[]{Long.toString(itemId)});

    }

}
