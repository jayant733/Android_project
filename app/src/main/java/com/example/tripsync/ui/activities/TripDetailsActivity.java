package com.example.tripsync.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.data.db.TripDatabaseHelper;
import com.example.tripsync.data.provider.TripContentProvider;

public class TripDetailsActivity extends AppCompatActivity {
    ImageView ivTripImage;
    TextView tvDestination, tvStartDate;
    TextView tvTripName, tvPlan, tvStatus, tvDuration, tvBudgetValue;
    Button btnBack, btnCollaborators, btnStartTrip, btnCompleteTrip,
            btnShareTrip, btnCopyPlan, btnDeleteTrip;
    SeekBar seekBudget;
    Switch switchNotifications;
    ProgressBar progressTrip;

    long tripId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);


        // Initialize Views
        tvTripName = findViewById(R.id.tvTripName);
        tvPlan = findViewById(R.id.tvPlan);
        tvStatus = findViewById(R.id.tvStatus);
        tvDuration = findViewById(R.id.tvDuration);
        tvBudgetValue = findViewById(R.id.tvBudgetValue);

        btnBack = findViewById(R.id.btnBack);
        btnCollaborators = findViewById(R.id.btnCollaborators);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnCompleteTrip = findViewById(R.id.btnCompleteTrip);

        btnCopyPlan = findViewById(R.id.btnCopyPlan);
        btnDeleteTrip = findViewById(R.id.btnDeleteTrip);

        ivTripImage = findViewById(R.id.ivTripImage);
        tvDestination = findViewById(R.id.tvDestination);
        tvStartDate = findViewById(R.id.tvStartDate);
        seekBudget = findViewById(R.id.seekBudget);

        progressTrip = findViewById(R.id.progressTrip);

        // Receive data
        String tripName = getIntent().getStringExtra("trip_name");
        tripId = getIntent().getLongExtra("trip_id", -1);

        if (tripName == null) tripName = "Trip Details";
        tvTripName.setText(tripName);

        loadItineraryFromDatabase();
        loadTripMetaData();

        // Budget Slider
        seekBudget.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressTrip.setProgress(progress);
                tvBudgetValue.setText("Budget Level: ₹ " + (progress * 1000));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Notifications

        // Start Trip
        btnStartTrip.setOnClickListener(v -> updateTripStatus("ONGOING"));

        // Complete Trip
        btnCompleteTrip.setOnClickListener(v -> updateTripStatus("COMPLETED"));

        // Share Trip


        // Copy Itinerary
        btnCopyPlan.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText(
                    "Trip Itinerary",
                    tvPlan.getText().toString());

            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Itinerary Copied", Toast.LENGTH_SHORT).show();
        });

        // Delete Trip (with confirmation)
        btnDeleteTrip.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Trip")
                    .setMessage("Are you sure you want to delete this trip?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        getContentResolver().delete(
                                TripContentProvider.TRIPS_URI,
                                TripDatabaseHelper.COLUMN_ID + "=?",
                                new String[]{String.valueOf(tripId)}
                        );
                        Toast.makeText(this, "Trip Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Collaborators
        btnCollaborators.setOnClickListener(v -> {
            Intent intent = new Intent(this, CollaboratorsActivity.class);
            intent.putExtra("trip_id", tripId);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadItineraryFromDatabase() {

        if (tripId == -1) {
            tvPlan.setText("No itinerary available.");
            return;
        }

        Cursor cursor = getContentResolver().query(
                TripContentProvider.ITINERARY_URI,
                null,
                TripDatabaseHelper.COLUMN_TRIP_ID + "=?",
                new String[]{String.valueOf(tripId)},
                TripDatabaseHelper.COLUMN_DAY_NUMBER + " ASC"
        );

        StringBuilder itineraryBuilder = new StringBuilder();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int day = cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_DAY_NUMBER));

                String description = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_DESCRIPTION));

                itineraryBuilder.append(description)
                        .append("\n\n");

            } while (cursor.moveToNext());

            cursor.close();
            tvPlan.setText(itineraryBuilder.toString());

        } else {
            tvPlan.setText("No itinerary added yet.");
        }
    }

    private void loadTripMetaData() {

        if (tripId == -1) return;

        Cursor cursor = getContentResolver().query(
                TripContentProvider.TRIPS_URI,
                null,
                TripDatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(tripId)},
                null
        );

        if (cursor == null || !cursor.moveToFirst()) return;

        String destination = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TripDatabaseHelper.COLUMN_DESTINATION));

        String startDateStr = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TripDatabaseHelper.COLUMN_START_DATE));

        String status = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        TripDatabaseHelper.COLUMN_STATUS));

        cursor.close();

        tvDestination.setText("Destination: " + destination);
        tvStartDate.setText("Start Date: " + startDateStr);

        // 🔥 AUTO STATUS BASED ON DATE
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            Date tripDate = sdf.parse(startDateStr);

            // Remove time difference effect
            long diff = tripDate.getTime() - today.getTime();
            long daysDiff = diff / (1000 * 60 * 60 * 24);

            if (daysDiff > 0) {
                status = "PLANNED";
                progressTrip.setProgress(10);

                if (btnStartTrip != null) btnStartTrip.setVisibility(View.GONE);
                if (btnCompleteTrip != null) btnCompleteTrip.setVisibility(View.GONE);

            } else if (daysDiff == 0) {
                status = "ONGOING";
                progressTrip.setProgress(50);

                if (btnStartTrip != null) btnStartTrip.setVisibility(View.GONE);
                if (btnCompleteTrip != null) btnCompleteTrip.setVisibility(View.VISIBLE);

            } else {
                status = "COMPLETED";
                progressTrip.setProgress(100);

                if (btnStartTrip != null) btnStartTrip.setVisibility(View.GONE);
                if (btnCompleteTrip != null) btnCompleteTrip.setVisibility(View.GONE);
            }

            // 🔥 Update DB automatically
            ContentValues values = new ContentValues();
            values.put(TripDatabaseHelper.COLUMN_STATUS, status);

            getContentResolver().update(
                    TripContentProvider.TRIPS_URI,
                    values,
                    TripDatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(tripId)}
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        tvStatus.setText(status);

        if ("ONGOING".equals(status)) {
            tvStatus.setBackgroundColor(0xFF10B981);
        } else if ("COMPLETED".equals(status)) {
            tvStatus.setBackgroundColor(0xFF3B82F6);
        } else {
            tvStatus.setBackgroundColor(0xFFF59E0B);
        }

        // 🔥 Duration
        Cursor itineraryCursor = getContentResolver().query(
                TripContentProvider.ITINERARY_URI,
                null,
                TripDatabaseHelper.COLUMN_TRIP_ID + "=?",
                new String[]{String.valueOf(tripId)},
                null
        );

        int days = 0;
        if (itineraryCursor != null) {
            days = itineraryCursor.getCount();
            itineraryCursor.close();
        }

        tvDuration.setText("Duration: " + days + " Days");
    }

    private void updateTripStatus(String newStatus) {

        ContentValues values = new ContentValues();
        values.put(TripDatabaseHelper.COLUMN_STATUS, newStatus);

        getContentResolver().update(
                TripContentProvider.TRIPS_URI,
                values,
                TripDatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(tripId)}
        );

        loadTripMetaData();
    }
}