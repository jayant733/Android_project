package com.example.tripsync.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripDetailsActivity extends AppCompatActivity {
    ImageView ivTripImage;
    TextView tvDestination, tvStartDate;
    TextView tvTripName, tvPlan, tvStatus, tvDuration;
    TextView tvTotalSpent, tvExpenseEntries, tvMemberCount;
    Button btnBack, btnCollaborators, btnStartTrip, btnCompleteTrip,
            btnMyGroups, btnDeleteTrip, btnUpdateTripImage;

    String tripDocId;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ActivityResultLauncher<String[]> tripImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

        tvTripName = findViewById(R.id.tvTripName);
        tvPlan = findViewById(R.id.tvPlan);
        tvStatus = findViewById(R.id.tvStatus);
        tvDuration = findViewById(R.id.tvDuration);
        tvDestination = findViewById(R.id.tvDestination);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvExpenseEntries = findViewById(R.id.tvExpenseEntries);
        tvMemberCount = findViewById(R.id.tvMemberCount);

        btnBack = findViewById(R.id.btnBack);
        btnCollaborators = findViewById(R.id.btnCollaborators);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnCompleteTrip = findViewById(R.id.btnCompleteTrip);
        btnMyGroups = findViewById(R.id.btnMyGroups);
        btnDeleteTrip = findViewById(R.id.btnDeleteTrip);
        btnUpdateTripImage = findViewById(R.id.btnUpdateTripImage);
        ivTripImage = findViewById(R.id.ivTripImage);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        tripDocId = getIntent().getStringExtra("trip_doc_id");

        tripImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null || tripDocId == null || auth.getCurrentUser() == null) {
                        return;
                    }

                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    ivTripImage.setImageURI(uri);
                    db.collection("users")
                            .document(auth.getCurrentUser().getUid())
                            .collection("trips")
                            .document(tripDocId)
                            .update("imageUri", uri.toString())
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "Trip image updated", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Could not update image", Toast.LENGTH_SHORT).show()
                            );
                }
        );

        tvPlan.setText("No trip details added yet.");
        tvTotalSpent.setText("₹0.00");
        tvExpenseEntries.setText("0 entries");
        tvMemberCount.setText("0 travelers");

        loadTripMetaData();
        loadSpendingSummary();

        btnUpdateTripImage.setOnClickListener(v ->
                tripImagePickerLauncher.launch(new String[]{"image/*"})
        );

        btnStartTrip.setOnClickListener(v -> updateTripStatus("ONGOING"));
        btnCompleteTrip.setOnClickListener(v -> updateTripStatus("COMPLETED"));

        btnMyGroups.setOnClickListener(v ->
                startActivity(new Intent(this, MyGroupActivity.class))
        );

        btnDeleteTrip.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Trip")
                    .setMessage("Are you sure you want to delete this trip?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteTrip())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnCollaborators.setOnClickListener(v -> {
            if (tripDocId == null) {
                Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, CollaboratorsActivity.class);
            intent.putExtra("trip_doc_id", tripDocId);
            intent.putExtra("trip_owner_user_id", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "");
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSpendingSummary();
    }

    private void loadTripMetaData() {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String tripName = documentSnapshot.getString("name");
                    String destination = documentSnapshot.getString("location");
                    String startDateStr = documentSnapshot.getString("startDate");
                    String details = documentSnapshot.getString("details");
                    String days = documentSnapshot.getString("days");
                    String imageUri = documentSnapshot.getString("imageUri");
                    String savedStatus = documentSnapshot.getString("status");
                    String status = (savedStatus == null || savedStatus.isEmpty())
                            ? resolveTripStatus(startDateStr)
                            : savedStatus;

                    tvTripName.setText(tripName != null ? tripName : "Trip Details");
                    tvDestination.setText("Destination: " + (destination != null ? destination : "Unknown"));
                    tvStartDate.setText("Start Date: " + (startDateStr != null ? startDateStr : "Unknown"));
                    tvPlan.setText(details != null && !details.isEmpty() ? details : "No trip details added yet.");
                    tvDuration.setText("Duration: " + ((days != null && !days.isEmpty()) ? days : "0") + " Days");

                    if (imageUri != null && !imageUri.isEmpty()) {
                        ivTripImage.setImageURI(Uri.parse(imageUri));
                    } else {
                        ivTripImage.setImageResource(android.R.color.transparent);
                    }

                    applyStatusUi(status);
                    updateRemoteStatus(status, false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load trip", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSpendingSummary() {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            return;
        }

        tripRef()
                .collection("groupMembers")
                .get()
                .addOnSuccessListener(memberSnapshot ->
                        {
                            int acceptedCount = 0;
                            for (com.google.firebase.firestore.DocumentSnapshot doc : memberSnapshot.getDocuments()) {
                                if ("ACCEPTED".equals(doc.getString("status"))) {
                                    acceptedCount++;
                                }
                            }
                            tvMemberCount.setText(acceptedCount + " travelers");
                        });

        tripRef()
                .collection("expenses")
                .get()
                .addOnSuccessListener(expenseSnapshot -> {
                    double totalSpent = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : expenseSnapshot.getDocuments()) {
                        Double amount = doc.getDouble("amount");
                        totalSpent += amount != null ? amount : 0;
                    }

                    tvTotalSpent.setText("₹" + String.format(Locale.US, "%.2f", totalSpent));
                    tvExpenseEntries.setText(expenseSnapshot.size() + " entries");
                });
    }

    private void updateTripStatus(String newStatus) {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            return;
        }

        updateRemoteStatus(newStatus, true);
        applyStatusUi(newStatus);
    }

    private void updateRemoteStatus(String status, boolean showToast) {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId)
                .update("status", status)
                .addOnSuccessListener(unused -> {
                    if (showToast) {
                        Toast.makeText(this, "Trip status updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not update trip status", Toast.LENGTH_SHORT).show()
                );
    }

    private void deleteTrip() {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Trip Deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not delete trip", Toast.LENGTH_SHORT).show()
                );
    }

    private String resolveTripStatus(String startDateStr) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date today = formatter.parse(formatter.format(new Date()));
            Date tripDate = formatter.parse(startDateStr);

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

    private void applyStatusUi(String status) {
        tvStatus.setText(status);

        if ("ONGOING".equals(status)) {
            tvStatus.setBackgroundColor(0xFF10B981);
            btnStartTrip.setVisibility(View.GONE);
            btnCompleteTrip.setVisibility(View.VISIBLE);
        } else if ("COMPLETED".equals(status)) {
            tvStatus.setBackgroundColor(0xFF3B82F6);
            btnStartTrip.setVisibility(View.GONE);
            btnCompleteTrip.setVisibility(View.GONE);
        } else {
            tvStatus.setBackgroundColor(0xFFF59E0B);
            btnStartTrip.setVisibility(View.VISIBLE);
            btnCompleteTrip.setVisibility(View.GONE);
        }
    }

    private com.google.firebase.firestore.DocumentReference tripRef() {
        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId);
    }
}
