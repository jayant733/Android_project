package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.data.db.TripDatabaseHelper;
import com.example.tripsync.data.provider.TripContentProvider;
import com.example.tripsync.service.TripReminderService;
import com.example.tripsync.ui.adapters.FeedAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TripListActivity extends AppCompatActivity {

    ListView listTrips, listFeed;
    ArrayList<String> feedTrips;
    FeedAdapter feedAdapter;

    FloatingActionButton fabCreateTrip;
    LinearLayout currentTripCard;

    TextView tvCurrentTrip, tvCurrentDestination, tvStats, tvUserEmail;
    ImageView ivProfileIcon;

    ArrayList<String> upcomingTrips;
    ArrayAdapter<String> adapter;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);

        listTrips = findViewById(R.id.listTrips);
        listFeed = findViewById(R.id.listFeed);
        fabCreateTrip = findViewById(R.id.fabCreateTrip);
        currentTripCard = findViewById(R.id.currentTripCard);

        tvCurrentTrip = findViewById(R.id.tvCurrentTrip);
        tvCurrentDestination = findViewById(R.id.tvCurrentDestination);
        tvStats = findViewById(R.id.tvStats);

        // 🔥 ADD THIS TEXTVIEW IN XML ALSO
        tvUserEmail = findViewById(R.id.tvUserEmail);

        ivProfileIcon = findViewById(R.id.ivProfileIcon);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ✅ SHOW LOGGED IN EMAIL
        if (mAuth.getCurrentUser() != null) {
            tvUserEmail.setText("Logged in as: " + mAuth.getCurrentUser().getEmail());
        }

        setupProfileSection();

        listTrips.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTrip = upcomingTrips.get(position);
            String tripName = selectedTrip.split(" - ")[0];

            long tripId = getTripIdByName(tripName);

            Intent intent = new Intent(this, TripDetailsActivity.class);
            intent.putExtra("trip_name", tripName);
            intent.putExtra("trip_id", tripId);
            startActivity(intent);
        });

        // 🔥 FIREBASE FEED
        feedTrips = new ArrayList<>();
        feedAdapter = new FeedAdapter(this, feedTrips);
        listFeed.setAdapter(feedAdapter);

        loadFeed();

        fabCreateTrip.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTripActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTripsFromDatabase();
        setupProfileSection();
    }

    private void loadTripsFromDatabase() {

        upcomingTrips = new ArrayList<>();
        boolean foundCurrent = false;
        Date today = new Date();

        Cursor cursor = getContentResolver().query(
                TripContentProvider.TRIPS_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null) {

            while (cursor.moveToNext()) {

                long tripId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(TripDatabaseHelper.COLUMN_ID));

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow(TripDatabaseHelper.COLUMN_NAME));

                String destination = cursor.getString(
                        cursor.getColumnIndexOrThrow(TripDatabaseHelper.COLUMN_DESTINATION));

                String startDateStr = cursor.getString(
                        cursor.getColumnIndexOrThrow(TripDatabaseHelper.COLUMN_START_DATE));

                try {
                    Date tripDate = sdf.parse(startDateStr);

                    long diff = tripDate.getTime() - today.getTime();
                    long daysRemaining = diff / (1000 * 60 * 60 * 24);

                    if (daysRemaining == 0) {

                        tvCurrentTrip.setText(name);
                        tvCurrentDestination.setText(destination + " (Starts Today)");
                        foundCurrent = true;

                        SharedPreferences reminderPrefs =
                                getSharedPreferences("TripReminderPrefs", MODE_PRIVATE);

                        boolean alreadyTriggered =
                                reminderPrefs.getBoolean("reminder_sent_" + tripId, false);

                        if (!alreadyTriggered) {

                            Intent intent = new Intent(this, TripReminderService.class);
                            intent.putExtra("trip_name", name);
                            startService(intent);

                            reminderPrefs.edit()
                                    .putBoolean("reminder_sent_" + tripId, true)
                                    .apply();
                        }

                    } else if (daysRemaining > 0) {

                        upcomingTrips.add(name + " - " + destination +
                                " (" + daysRemaining + " days remaining)");
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            cursor.close();
        }

        if (!foundCurrent) {
            tvCurrentTrip.setText("No Active Trip");
            tvCurrentDestination.setText("Start planning today!");
        }

        adapter = new TripAdapter();
        listTrips.setAdapter(adapter);

        tvStats.setText("Upcoming Trips: " + upcomingTrips.size());
    }

    private void setupProfileSection() {

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);

        ivProfileIcon.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(this, ivProfileIcon);
            popup.getMenu().add("Profile");
            popup.getMenu().add("Past Trips");
            popup.getMenu().add("My Group");
            popup.getMenu().add("Logout");

            popup.setOnMenuItemClickListener(item -> {

                if (item.getTitle().equals("Profile")) {
                    startActivity(new Intent(this, ProfileActivity.class));

                } else if (item.getTitle().equals("My Group")) {
                    startActivity(new Intent(this, MyGroupActivity.class));

                } else if (item.getTitle().equals("Logout")) {

                    // 🔥 LOGOUT FIX
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
                }

                return true;
            });

            popup.show();
        });
    }

    private long getTripIdByName(String tripName) {

        Cursor cursor = getContentResolver().query(
                TripContentProvider.TRIPS_URI,
                null,
                TripDatabaseHelper.COLUMN_NAME + "=?",
                new String[]{tripName},
                null
        );

        long tripId = -1;

        if (cursor != null && cursor.moveToFirst()) {
            tripId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                            TripDatabaseHelper.COLUMN_ID));
            cursor.close();
        }

        return tripId;
    }

    private void loadFeed() {

        db.collection("feed")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Toast.makeText(this, "Feed Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) return;

                    feedTrips.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        String name = doc.getString("name");
                        String location = doc.getString("location");
                        String email = doc.getString("email");

                        if (name == null) name = "Unknown Trip";
                        if (location == null) location = "Unknown Location";
                        if (email == null) email = "Unknown User";

                        feedTrips.add(
                                "📍 " + name +
                                        "\n📌 " + location +
                                        "\n👤 " + email
                        );
                    }

                    feedAdapter.notifyDataSetChanged();
                });
    }

    private class TripAdapter extends ArrayAdapter<String> {

        public TripAdapter() {
            super(TripListActivity.this, 0, upcomingTrips);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater()
                        .inflate(R.layout.item_trip, parent, false);
            }

            TextView name = convertView.findViewById(R.id.tvTripItemName);
            ImageView menu = convertView.findViewById(R.id.ivMenu);

            String item = upcomingTrips.get(position);
            name.setText(item);

            menu.setOnClickListener(v -> {

                PopupMenu popup = new PopupMenu(TripListActivity.this, menu);
                popup.getMenu().add("Share");

                popup.setOnMenuItemClickListener(menuItem -> {

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, item);
                    startActivity(Intent.createChooser(shareIntent, "Share via"));

                    return true;
                });

                popup.show();
            });

            return convertView;
        }

    }
}