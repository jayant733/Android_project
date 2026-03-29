package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.ui.adapters.FeedAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TripListActivity extends AppCompatActivity {

    ListView listTrips, listFeed;
    ArrayList<String> feedTrips;
    FeedAdapter feedAdapter;

    FloatingActionButton fabCreateTrip;
    LinearLayout currentTripCard;

    TextView tvCurrentTrip, tvCurrentDestination, tvStats, tvUserEmail;
    ImageView ivProfileIcon;

    ArrayList<String> upcomingTrips;
    ArrayList<String> upcomingTripDocIds;
    ArrayAdapter<String> adapter;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String currentTripDocId;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

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
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        updateUserHeader();
        setupProfileSection();

        listTrips.setOnItemClickListener((parent, view, position, id) -> openTripDetails(upcomingTripDocIds.get(position)));
        currentTripCard.setOnClickListener(v -> {
            if (currentTripDocId != null) {
                openTripDetails(currentTripDocId);
            }
        });

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
        updateUserHeader();
        loadTripsFromCloud();
        setupProfileSection();
    }

    private void loadTripsFromCloud() {
        upcomingTrips = new ArrayList<>();
        upcomingTripDocIds = new ArrayList<>();
        currentTripDocId = null;
        Date today = getStartOfToday();

        if (mAuth.getCurrentUser() == null) {
            tvCurrentTrip.setText("No Active Trip");
            tvCurrentDestination.setText("Log in to view your trips");
            listTrips.setAdapter(new TripAdapter());
            tvStats.setText("Upcoming Trips: 0 | Past Trips: 0");
            return;
        }

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("trips")
                .orderBy("startDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pastTripCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("name");
                        String destination = doc.getString("location");
                        String startDateStr = doc.getString("startDate");
                        String savedStatus = doc.getString("status");

                        if (name == null || destination == null || startDateStr == null) {
                            continue;
                        }

                        String status = (savedStatus == null || savedStatus.isEmpty())
                                ? resolveTripStatus(startDateStr)
                                : savedStatus;

                        try {
                            Date tripDate = sdf.parse(startDateStr);
                            if (tripDate == null) {
                                continue;
                            }

                            if ("COMPLETED".equals(status)) {
                                pastTripCount++;
                                continue;
                            }

                            if ("ONGOING".equals(status)) {
                                if (currentTripDocId == null) {
                                    currentTripDocId = doc.getId();
                                    tvCurrentTrip.setText(name);
                                    tvCurrentDestination.setText(destination + " (Trip in progress)");
                                }
                                continue;
                            }

                            int compare = tripDate.compareTo(today);
                            if (compare <= 0 && currentTripDocId == null) {
                                currentTripDocId = doc.getId();
                                tvCurrentTrip.setText(name);
                                tvCurrentDestination.setText(destination + " (Starts now)");
                            } else {
                                long daysRemaining = Math.max(0, (tripDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
                                upcomingTrips.add(name + " - " + destination + " (" + daysRemaining + " days remaining)");
                                upcomingTripDocIds.add(doc.getId());
                            }
                        } catch (ParseException e) {
                            Toast.makeText(this, "Invalid trip date found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (currentTripDocId == null) {
                        tvCurrentTrip.setText("No Active Trip");
                        tvCurrentDestination.setText("Start planning today!");
                    }

                    adapter = new TripAdapter();
                    listTrips.setAdapter(adapter);
                    tvStats.setText("Upcoming Trips: " + upcomingTrips.size() + " | Past Trips: " + pastTripCount);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load your trips", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupProfileSection() {
        ivProfileIcon.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, ivProfileIcon);
            popup.getMenu().add("Profile");
            popup.getMenu().add("Past Trips");
            popup.getMenu().add("My Group");
            popup.getMenu().add("Logout");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Profile")) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else if (item.getTitle().equals("Past Trips")) {
                    startActivity(new Intent(this, PastTripsActivity.class));
                } else if (item.getTitle().equals("My Group")) {
                    startActivity(new Intent(this, MyGroupActivity.class));
                } else if (item.getTitle().equals("Logout")) {
                    FirebaseAuth.getInstance().signOut();
                    getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit().clear().apply();

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

    private void loadFeed() {
        db.collection("feed")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Feed Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) {
                        return;
                    }

                    feedTrips.clear();
                    String currentUserId = mAuth.getCurrentUser() != null
                            ? mAuth.getCurrentUser().getUid()
                            : null;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String ownerUserId = doc.getString("userId");
                        if (currentUserId != null && currentUserId.equals(ownerUserId)) {
                            continue;
                        }

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

    private void updateUserHeader() {
        SharedPreferences sessionPrefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        SharedPreferences profilePrefs = getSharedPreferences("UserProfile", MODE_PRIVATE);

        String savedName = profilePrefs.getString("user_name", "");
        String savedEmail = sessionPrefs.getString("user_email", "");

        if ((savedEmail == null || savedEmail.isEmpty()) && mAuth.getCurrentUser() != null) {
            savedEmail = mAuth.getCurrentUser().getEmail();
        }

        if (savedName != null && !savedName.isEmpty()) {
            tvUserEmail.setText(savedName + "\n" + savedEmail);
        } else if (savedEmail != null && !savedEmail.isEmpty()) {
            tvUserEmail.setText("Logged in as:\n" + savedEmail);
        } else {
            tvUserEmail.setText("TripSync User");
        }
    }

    private void openTripDetails(String tripDocId) {
        Intent intent = new Intent(this, TripDetailsActivity.class);
        intent.putExtra("trip_doc_id", tripDocId);
        startActivity(intent);
    }

    private String resolveTripStatus(String startDateStr) {
        try {
            Date today = getStartOfToday();
            Date tripDate = sdf.parse(startDateStr);

            if (tripDate == null || today == null) {
                return "PLANNED";
            }

            int compare = tripDate.compareTo(today);
            if (compare > 0) {
                return "PLANNED";
            }
            if (compare == 0) {
                return "ONGOING";
            }
            return "COMPLETED";
        } catch (Exception e) {
            return "PLANNED";
        }
    }

    private Date getStartOfToday() {
        try {
            return sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            return new Date();
        }
    }

    private class TripAdapter extends ArrayAdapter<String> {

        public TripAdapter() {
            super(TripListActivity.this, 0, upcomingTrips);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_trip, parent, false);
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
