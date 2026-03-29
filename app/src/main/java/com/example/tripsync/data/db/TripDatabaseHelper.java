package com.example.tripsync.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TripDatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_EXPENSES = "expenses";
    public static final String COLUMN_EXPENSE_ID = "_id";
    public static final String COLUMN_EXPENSE_TRIP_ID = "trip_id";
    public static final String COLUMN_PAID_BY = "paid_by";
    public static final String COLUMN_AMOUNT = "amount";

    public static final String DATABASE_NAME = "trips.db";
    public static final int DATABASE_VERSION = 5;   // 🔥 increase version

    // ===== TRIPS TABLE =====
    public static final String TABLE_TRIPS = "trips";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "trip_name";
    public static final String COLUMN_DESTINATION = "destination";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_START_DATE = "start_date";  // 🔥 NEW

    // ===== ITINERARY TABLE =====
    public static final String TABLE_ITINERARY = "itinerary";
    public static final String COLUMN_TRIP_ID = "trip_id";
    public static final String COLUMN_DAY_NUMBER = "day_number";
    public static final String COLUMN_DESCRIPTION = "description";

    // ===== COLLABORATORS TABLE =====
    public static final String TABLE_COLLABORATORS = "collaborators";
    public static final String COLUMN_COLLAB_NAME = "name";
    public static final String COLUMN_COLLAB_EMAIL = "email";

    public TripDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_TRIPS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_DESTINATION + " TEXT, " +
                COLUMN_STATUS + " TEXT DEFAULT 'PLANNED', " +
                COLUMN_START_DATE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ITINERARY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TRIP_ID + " INTEGER, " +
                COLUMN_DAY_NUMBER + " INTEGER, " +
                COLUMN_DESCRIPTION + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_COLLABORATORS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TRIP_ID + " INTEGER, " +
                COLUMN_COLLAB_NAME + " TEXT, " +
                COLUMN_COLLAB_EMAIL + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" +
                COLUMN_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EXPENSE_TRIP_ID + " INTEGER, " +
                COLUMN_PAID_BY + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_DESCRIPTION + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITINERARY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLABORATORS);

        onCreate(db);
    }
}