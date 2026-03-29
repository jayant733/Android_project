package com.example.tripsync.ui.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateTripActivity extends AppCompatActivity {

    EditText etTripName, etTripDetails, etTripDays, etLocation, etStartDate;
    Button btnCreateTrip, btnBack, btnSelectImage;
    ImageView ivTripImage;

    Button btnPostTrip;

    FirebaseFirestore db;
    FirebaseAuth auth;

    Uri selectedImageUri = null;
    private ActivityResultLauncher<String[]> tripImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);

        etTripName = findViewById(R.id.etTripName);
        etTripDetails = findViewById(R.id.etTripDetails);
        etTripDays = findViewById(R.id.etTripDays);
        etLocation = findViewById(R.id.etLocation);
        etStartDate = findViewById(R.id.etStartDate);

        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        btnPostTrip = findViewById(R.id.btnPostTrip);
        btnBack = findViewById(R.id.btnBack);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        ivTripImage = findViewById(R.id.ivTripImage);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tripImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) {
                        return;
                    }

                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    selectedImageUri = uri;
                    ivTripImage.setImageURI(uri);
                }
        );

        etStartDate.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, y, m, d) -> {

                        // Format to yyyy-MM-dd
                        String formattedDate = String.format("%04d-%02d-%02d",
                                y, m + 1, d);

                        etStartDate.setText(formattedDate);
                    },
                    year, month, day);

            dialog.show();
        });

        btnSelectImage.setOnClickListener(v -> tripImagePickerLauncher.launch(new String[]{"image/*"}));
        ivTripImage.setOnClickListener(v -> tripImagePickerLauncher.launch(new String[]{"image/*"}));

        btnCreateTrip.setOnClickListener(v -> saveTrip(false));
        btnPostTrip.setOnClickListener(v -> saveTrip(true));

        btnBack.setOnClickListener(v -> finish());
    }

    private void saveTrip(boolean isPost) {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etTripName.getText().toString().trim();
        String details = etTripDetails.getText().toString().trim();
        String days = etTripDays.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String startDate = etStartDate.getText().toString().trim();

        if (name.isEmpty() || location.isEmpty() || startDate.isEmpty()) {
            Toast.makeText(this, "Fill trip name, location, and start date", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        String tripStatus = resolveTripStatus(startDate);

        Map<String, Object> trip = new HashMap<>();
        trip.put("name", name);
        trip.put("details", details);
        trip.put("days", days);
        trip.put("location", location);
        trip.put("startDate", startDate);
        trip.put("status", tripStatus);
        trip.put("userId", userId);
        trip.put("email", email);
        trip.put("imageUri", selectedImageUri != null ? selectedImageUri.toString() : null);
        trip.put("timestamp", System.currentTimeMillis());

        createTripInCloud(userId, trip, isPost);
    }

    private void createTripInCloud(String userId, Map<String, Object> trip, boolean isPost) {
        db.collection("users")
                .document(userId)
                .collection("trips")
                .add(trip)
                .addOnSuccessListener(documentReference -> {
                    trip.put("tripDocId", documentReference.getId());

                    if (isPost) {
                        postToFeed(trip);
                    } else {
                        Toast.makeText(this, "Trip created", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not save trip", Toast.LENGTH_SHORT).show()
                );
    }

    private void postToFeed(Map<String, Object> trip) {

        db.collection("feed")
                .add(trip)
                .addOnSuccessListener(feedDoc -> {
                    Toast.makeText(this, "Trip created and posted to the global feed", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Trip created, but feed posting failed", Toast.LENGTH_SHORT).show()
                );
    }

    private String resolveTripStatus(String startDate) {
        try {
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            formatter.setLenient(false);
            java.util.Date today = formatter.parse(formatter.format(new java.util.Date()));
            java.util.Date tripDate = formatter.parse(startDate);

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

    private void navigateBack() {
        Intent intent = new Intent(this, TripListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
