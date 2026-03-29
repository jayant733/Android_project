package com.example.tripsync.data.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.example.tripsync.data.db.TripDatabaseHelper;

public class TripContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.tripsync.provider";

    public static final Uri TRIPS_URI =
            Uri.parse("content://" + AUTHORITY + "/trips");

    public static final Uri ITINERARY_URI =
            Uri.parse("content://" + AUTHORITY + "/itinerary");

    public static final Uri COLLABORATORS_URI =
            Uri.parse("content://" + AUTHORITY + "/collaborators");

    public static final Uri EXPENSES_URI =
            Uri.parse("content://" + AUTHORITY + "/expenses");

    private static final int TRIPS = 1;
    private static final int ITINERARY = 2;
    private static final int COLLABORATORS = 3;
    private static final int EXPENSES = 4;

    private static final UriMatcher uriMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "trips", TRIPS);
        uriMatcher.addURI(AUTHORITY, "itinerary", ITINERARY);
        uriMatcher.addURI(AUTHORITY, "collaborators", COLLABORATORS);
        uriMatcher.addURI(AUTHORITY, "expenses", EXPENSES);
    }

    private TripDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new TripDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        switch (uriMatcher.match(uri)) {

            case TRIPS:
                return db.query(TripDatabaseHelper.TABLE_TRIPS,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);

            case ITINERARY:
                return db.query(TripDatabaseHelper.TABLE_ITINERARY,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);

            case COLLABORATORS:
                return db.query(TripDatabaseHelper.TABLE_COLLABORATORS,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);

            case EXPENSES:
                return db.query(TripDatabaseHelper.TABLE_EXPENSES,
                        projection, selection, selectionArgs,
                        null, null, sortOrder);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id;

        switch (uriMatcher.match(uri)) {

            case TRIPS:
                id = db.insert(TripDatabaseHelper.TABLE_TRIPS, null, values);
                return ContentUris.withAppendedId(TRIPS_URI, id);

            case ITINERARY:
                id = db.insert(TripDatabaseHelper.TABLE_ITINERARY, null, values);
                return ContentUris.withAppendedId(ITINERARY_URI, id);

            case COLLABORATORS:
                id = db.insert(TripDatabaseHelper.TABLE_COLLABORATORS, null, values);
                return ContentUris.withAppendedId(COLLABORATORS_URI, id);

            case EXPENSES:
                id = db.insert(TripDatabaseHelper.TABLE_EXPENSES, null, values);
                return ContentUris.withAppendedId(EXPENSES_URI, id);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection,
                      String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {

            case TRIPS:
                return db.delete(TripDatabaseHelper.TABLE_TRIPS,
                        selection, selectionArgs);

            case ITINERARY:
                return db.delete(TripDatabaseHelper.TABLE_ITINERARY,
                        selection, selectionArgs);

            case COLLABORATORS:
                return db.delete(TripDatabaseHelper.TABLE_COLLABORATORS,
                        selection, selectionArgs);

            case EXPENSES:
                return db.delete(TripDatabaseHelper.TABLE_EXPENSES,
                        selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {

            case TRIPS:
                return db.update(TripDatabaseHelper.TABLE_TRIPS,
                        values, selection, selectionArgs);

            case ITINERARY:
                return db.update(TripDatabaseHelper.TABLE_ITINERARY,
                        values, selection, selectionArgs);

            case COLLABORATORS:
                return db.update(TripDatabaseHelper.TABLE_COLLABORATORS,
                        values, selection, selectionArgs);

            case EXPENSES:
                return db.update(TripDatabaseHelper.TABLE_EXPENSES,
                        values, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}