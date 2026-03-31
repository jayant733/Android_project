package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.ui.adapters.FeedAdapter;
import com.example.tripsync.ui.common.EdgeToEdgeHelper;
import com.example.tripsync.ui.common.LocalUserStore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class TripListActivity extends AppCompatActivity {

    ListView listTrips, listFeed;
    ArrayList<com.example.tripsync.data.model.FeedPost> feedTrips;
    FeedAdapter feedAdapter;

    FloatingActionButton fabCreateTrip;
    LinearLayout currentTripCard;

    TextView tvCurrentTrip, tvCurrentDestination, tvStats, tvUserEmail;
    ImageView ivProfileIcon, ivCurrentTripCover;

    ArrayList<String> upcomingTrips;
    ArrayList<String> upcomingTripDocIds;
    ArrayList<String> upcomingTripImageUris;
    ArrayAdapter<String> adapter;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String currentTripDocId;
    boolean isNameDialogShowing;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);
        EdgeToEdgeHelper.apply(this);

        listTrips = findViewById(R.id.listTrips);
        listFeed = findViewById(R.id.listFeed);
        fabCreateTrip = findViewById(R.id.fabCreateTrip);
        currentTripCard = findViewById(R.id.currentTripCard);
        tvCurrentTrip = findViewById(R.id.tvCurrentTrip);
        tvCurrentDestination = findViewById(R.id.tvCurrentDestination);
        tvStats = findViewById(R.id.tvStats);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        ivCurrentTripCover = findViewById(R.id.ivCurrentTripCover);

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
        ensureUserNameAvailable();
        updateUserHeader();
        loadTripsFromCloud();
        setupProfileSection();
    }

    private void loadTripsFromCloud() {
        upcomingTrips = new ArrayList<>();
        upcomingTripDocIds = new ArrayList<>();
        upcomingTripImageUris = new ArrayList<>();
        currentTripDocId = null;
        Date today = getStartOfToday();

        if (mAuth.getCurrentUser() == null) {
            tvCurrentTrip.setText("No Active Trip");
            tvCurrentDestination.setText("Log in to view your trips");
            ivCurrentTripCover.setImageDrawable(null);
            listTrips.setAdapter(new TripAdapter());
            setListViewHeightBasedOnChildren(listTrips);
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
                        String imageUri = doc.getString("imageUri");

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
                                    loadImageIntoView(ivCurrentTripCover, imageUri);
                                }
                                continue;
                            }

                            int compare = tripDate.compareTo(today);
                            if (compare <= 0 && currentTripDocId == null) {
                                currentTripDocId = doc.getId();
                                tvCurrentTrip.setText(name);
                                tvCurrentDestination.setText(destination + " (Starts now)");
                                loadImageIntoView(ivCurrentTripCover, imageUri);
                            } else {
                                long daysRemaining = Math.max(0, (tripDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
                                upcomingTrips.add(name + " - " + destination + " (" + daysRemaining + " days remaining)");
                                upcomingTripDocIds.add(doc.getId());
                                upcomingTripImageUris.add(imageUri != null ? imageUri : "");
                            }
                        } catch (ParseException e) {
                            Toast.makeText(this, "Invalid trip date found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (currentTripDocId == null) {
                        tvCurrentTrip.setText("No Active Trip");
                        tvCurrentDestination.setText("Start planning today!");
                        ivCurrentTripCover.setImageDrawable(null);
                    }

                    adapter = new TripAdapter();
                    listTrips.setAdapter(adapter);
                    setListViewHeightBasedOnChildren(listTrips);
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
                    LocalUserStore.clearSession(this);

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
                    ArrayList<DocumentSnapshot> visibleDocs = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String ownerUserId = doc.getString("userId");
                        if (currentUserId != null && currentUserId.equals(ownerUserId)) {
                            continue;
                        }
                        visibleDocs.add(doc);
                    }

                    if (visibleDocs.isEmpty()) {
                        feedAdapter.notifyDataSetChanged();
                        setListViewHeightBasedOnChildren(listFeed);
                        return;
                    }

                    if (currentUserId == null) {
                        for (DocumentSnapshot doc : visibleDocs) {
                            feedTrips.add(buildFeedPost(doc, false));
                        }
                        sortFeedTrips();
                        feedAdapter.notifyDataSetChanged();
                        setListViewHeightBasedOnChildren(listFeed);
                        return;
                    }

                    final int[] pending = {visibleDocs.size()};
                    for (DocumentSnapshot doc : visibleDocs) {
                        db.collection("feed")
                                .document(doc.getId())
                                .collection("ratings")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(ratingDoc -> {
                                    feedTrips.add(buildFeedPost(doc, ratingDoc.exists()));
                                    pending[0]--;
                                    if (pending[0] == 0) {
                                        sortFeedTrips();
                                        feedAdapter.notifyDataSetChanged();
                                        setListViewHeightBasedOnChildren(listFeed);
                                    }
                                })
                                .addOnFailureListener(fetchError -> {
                                    feedTrips.add(buildFeedPost(doc, false));
                                    pending[0]--;
                                    if (pending[0] == 0) {
                                        sortFeedTrips();
                                        feedAdapter.notifyDataSetChanged();
                                        setListViewHeightBasedOnChildren(listFeed);
                                    }
                                });
                    }
                });
    }

    private com.example.tripsync.data.model.FeedPost buildFeedPost(DocumentSnapshot doc, boolean hasUserRated) {
        String ownerUserId = doc.getString("userId");
        String name = doc.getString("name");
        String location = doc.getString("location");
        String details = doc.getString("details");
        String email = doc.getString("email");
        String imageUri = doc.getString("imageUri");
        Double averageRating = doc.getDouble("averageRating");
        Long ratingCount = doc.getLong("ratingCount");
        Long timestamp = doc.getLong("timestamp");

        if (name == null) name = "Unknown Trip";
        if (location == null) location = "Unknown Location";
        if (email == null) email = "Unknown User";

        return new com.example.tripsync.data.model.FeedPost(
                doc.getId(),
                name,
                location,
                details != null ? details : "",
                email,
                ownerUserId != null ? ownerUserId : "",
                imageUri != null ? imageUri : "",
                averageRating != null ? averageRating.floatValue() : 0f,
                ratingCount != null ? ratingCount.intValue() : 0,
                hasUserRated,
                timestamp != null ? timestamp : 0L
        );
    }

    private void sortFeedTrips() {
        feedTrips.sort((first, second) -> {
            if (first.hasUserRated != second.hasUserRated) {
                return first.hasUserRated ? 1 : -1;
            }
            return Comparator.comparingLong((com.example.tripsync.data.model.FeedPost post) -> post.timestamp)
                    .reversed()
                    .compare(first, second);
        });
    }

    private void updateUserHeader() {
        String savedEmail = LocalUserStore.getSessionEmail(this, "");

        if ((savedEmail == null || savedEmail.isEmpty()) && mAuth.getCurrentUser() != null) {
            savedEmail = mAuth.getCurrentUser().getEmail();
        }

        String savedName = LocalUserStore.getProfileName(this, savedEmail, "");

        if (savedName != null && !savedName.isEmpty()) {
            tvUserEmail.setText(savedName + "\n" + savedEmail);
        } else if (savedEmail != null && !savedEmail.isEmpty()) {
            tvUserEmail.setText("Logged in as:\n" + savedEmail);
        } else {
            tvUserEmail.setText("TripSync User");
        }

        updateProfileImage(savedEmail);
    }

    private void updateProfileImage(String email) {
        String savedImage = LocalUserStore.getProfileImage(this, email);

        if (savedImage == null || savedImage.trim().isEmpty()) {
            ivProfileIcon.setImageDrawable(null);
            return;
        }

        try {
            ivProfileIcon.setImageURI(Uri.parse(savedImage));
        } catch (Exception e) {
            ivProfileIcon.setImageDrawable(null);
        }
    }

    private void loadImageIntoView(ImageView imageView, String imageUri) {
        imageView.setImageURI(null);
        if (imageUri == null || imageUri.trim().isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }

        try {
            imageView.setImageURI(Uri.parse(imageUri));
        } catch (Exception e) {
            imageView.setImageDrawable(null);
        }
    }

    private void ensureUserNameAvailable() {
        String currentEmail = LocalUserStore.getSessionEmail(
                this,
                mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : ""
        );
        String savedName = LocalUserStore.getProfileName(this, currentEmail, "");

        if (savedName != null && !savedName.trim().isEmpty()) {
            isNameDialogShowing = false;
            return;
        }

        if (isNameDialogShowing) {
            return;
        }

        isNameDialogShowing = true;

        EditText input = new EditText(this);
        input.setHint("Enter your name");
        input.setTextColor(0xFF111827);
        input.setHintTextColor(0xFF9CA3AF);
        input.setPadding(32, 24, 32, 24);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("What is your name?")
                .setMessage("Your name will be shown on the home screen and in your profile.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnDismissListener(d -> isNameDialogShowing = false);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String enteredName = input.getText().toString().trim();

            if (enteredName.isEmpty()) {
                input.setError("Please enter your name");
                return;
            }

            LocalUserStore.saveProfileName(this, currentEmail, enteredName);
            updateUserHeader();
            Toast.makeText(this, "Welcome, " + enteredName, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
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

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * Math.max(0, listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void deleteUpcomingTrip(String tripDocId) {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId)
                .delete()
                .addOnSuccessListener(unused -> {
                    deleteTripFromFeed(tripDocId);
                    loadTripsFromCloud();
                    Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not delete trip", Toast.LENGTH_SHORT).show()
                );
    }

    private void deleteTripFromFeed(String tripDocId) {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        db.collection("feed")
                .whereEqualTo("tripDocId", tripDocId)
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        snapshot.getReference().delete();
                    }
                });
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
            TextView destination = convertView.findViewById(R.id.tvTripItemDestination);
            ImageView menu = convertView.findViewById(R.id.ivMenu);
            ImageView cover = convertView.findViewById(R.id.ivTripItemCover);

            String item = upcomingTrips.get(position);
            String[] primarySplit = item.split(" - ", 2);
            String tripName = primarySplit.length > 0 ? primarySplit[0] : item;
            String meta = primarySplit.length > 1 ? primarySplit[1] : "";

            name.setText(tripName);
            destination.setText(meta);

            String imageUri = upcomingTripImageUris.size() > position ? upcomingTripImageUris.get(position) : "";
            loadImageIntoView(cover, imageUri);

            menu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(TripListActivity.this, menu);
                popup.getMenu().add("Share");
                popup.getMenu().add("Delete Trip");

                popup.setOnMenuItemClickListener(menuItem -> {
                    if ("Delete Trip".contentEquals(menuItem.getTitle())) {
                        new AlertDialog.Builder(TripListActivity.this)
                                .setTitle("Delete trip?")
                                .setMessage("This will remove the trip from your list and your travel feed post.")
                                .setPositiveButton("Delete", (dialog, which) ->
                                        deleteUpcomingTrip(upcomingTripDocIds.get(position)))
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }

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
