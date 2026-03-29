package com.example.tripsync.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    TextView tvTripName, tvPlan, tvStatus, tvDuration, tvBudgetValue;
    Button btnBack, btnCollaborators, btnStartTrip, btnCompleteTrip,
            btnCopyPlan, btnDeleteTrip;
    SeekBar seekBudget;
    ProgressBar progressTrip;

    String tripDocId;
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        tripDocId = getIntent().getStringExtra("trip_doc_id");

        tvPlan.setText("No trip details added yet.");

        loadTripMetaData();

        seekBudget.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressTrip.setProgress(progress);
                tvBudgetValue.setText("Budget Level: ₹ " + (progress * 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        btnStartTrip.setOnClickListener(v -> updateTripStatus("ONGOING"));
        btnCompleteTrip.setOnClickListener(v -> updateTripStatus("COMPLETED"));

        btnCopyPlan.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Trip Details", tvPlan.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Trip details copied", Toast.LENGTH_SHORT).show();
        });

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

            android.content.Intent intent = new android.content.Intent(this, CollaboratorsActivity.class);
            intent.putExtra("trip_doc_id", tripDocId);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
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
                    String status = resolveTripStatus(startDateStr);

                    tvTripName.setText(tripName != null ? tripName : "Trip Details");
                    tvDestination.setText("Destination: " + (destination != null ? destination : "Unknown"));
                    tvStartDate.setText("Start Date: " + (startDateStr != null ? startDateStr : "Unknown"));
                    tvPlan.setText(details != null && !details.isEmpty() ? details : "No trip details added yet.");
                    tvDuration.setText("Duration: " + ((days != null && !days.isEmpty()) ? days : "0") + " Days");

                    if (imageUri != null && !imageUri.isEmpty()) {
                        ivTripImage.setImageURI(Uri.parse(imageUri));
                    }

                    applyStatusUi(status);
                    updateRemoteStatus(status);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load trip", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateTripStatus(String newStatus) {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            return;
        }

        updateRemoteStatus(newStatus);
        applyStatusUi(newStatus);
        Toast.makeText(this, "Trip status updated", Toast.LENGTH_SHORT).show();
    }

    private void updateRemoteStatus(String status) {
        if (tripDocId == null || auth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId)
                .update("status", status);
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
            progressTrip.setProgress(50);
            tvStatus.setBackgroundColor(0xFF10B981);
            btnStartTrip.setVisibility(View.GONE);
            btnCompleteTrip.setVisibility(View.VISIBLE);
        } else if ("COMPLETED".equals(status)) {
            progressTrip.setProgress(100);
            tvStatus.setBackgroundColor(0xFF3B82F6);
            btnStartTrip.setVisibility(View.GONE);
            btnCompleteTrip.setVisibility(View.GONE);
        } else {
            progressTrip.setProgress(10);
            tvStatus.setBackgroundColor(0xFFF59E0B);
            btnStartTrip.setVisibility(View.VISIBLE);
            btnCompleteTrip.setVisibility(View.GONE);
        }
    }
}
