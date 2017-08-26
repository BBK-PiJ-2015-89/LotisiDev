package testapp.silencertestapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by graemewilkinson on 26/08/2017.
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
    public static final int DB_VER = 1;
    public static final String[] ALL_FIELDS = {ITEM_ID_FIELD, WIFI_NAME_FIELD, START_TIME_FIELD, END_TIME_FIELD};

    public static final String CREATE_TBL = "CREATE table "+CONDITIONS_TBL+
            " ("+ITEM_ID_FIELD+" INTEGER PRIMARY KEY AUTOINCREMENT, "+ WIFI_NAME_FIELD+" TEXT NOT NULL, "+
            START_TIME_FIELD+" INTEGER NOT NULL, " +END_TIME_FIELD+" INTEGER NOT NULL)";

    public class ConditionDBOpenHelper extends SQLiteOpenHelper{

        public ConditionDBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_VER);
        }

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

    public void openWriteDB(){
        db = dpOpenHelper.getWritableDatabase();
    }

    public void openReadOnlyDB(){
        db = dpOpenHelper.getReadableDatabase();
    }

    public void closeDB(){
        db.close();
        db = null;
    }

    //SELECT * FROM CONDITIONS_TBL
    public Cursor getAllItems(){
        Cursor cursor = db.query(CONDITIONS_TBL, ALL_FIELDS, null, null, null, null, null);
        return cursor;
    }

    //SELECT * FROM CONDITIONS_TBL WHERE _id = ?;
    public Cursor getConditionByID(long item_Id){
        Cursor cursor = db.query(CONDITIONS_TBL, ALL_FIELDS, ITEM_ID_FIELD + " = ?",
                new String[]{Long.toString(item_Id)}, null, null, null);
                return cursor;
    }

    public long addCondition(String wifiName, long start_Time, long end_time ){
        ContentValues contentValues = new ContentValues();
        contentValues.put(WIFI_NAME_FIELD, wifiName);
        contentValues.put(START_TIME_FIELD, start_Time);
        contentValues.put(END_TIME_FIELD, end_time);

       return db.insert(CONDITIONS_TBL, null, contentValues);
    }

    public int updateConditionById(int itemId, String wifiName, long start_Time, long end_Time){
        ContentValues contentValues = new ContentValues();
        contentValues.put(WIFI_NAME_FIELD, wifiName);
        contentValues.put(START_TIME_FIELD, start_Time);
        contentValues.put(END_TIME_FIELD, end_Time);

        return db.update(CONDITIONS_TBL, contentValues, ITEM_ID_FIELD+" = ?", new String[]{Integer.toString(itemId)});

    }

    public void removeConditionById(int itemId){
        db.delete(CONDITIONS_TBL, ITEM_ID_FIELD+" = ?", new String[]{Integer.toString(itemId)});

    }

}
