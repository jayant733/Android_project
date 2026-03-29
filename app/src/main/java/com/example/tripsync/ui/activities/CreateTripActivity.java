package com.example.tripsync.ui.activities;

import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.data.db.TripDatabaseHelper;
import com.example.tripsync.data.provider.TripContentProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import java.util.Calendar;

public class CreateTripActivity extends AppCompatActivity {

    EditText etTripName, etTripDetails, etTripDays, etLocation, etStartDate;
    Button btnCreateTrip, btnBack, btnSelectImage;
    ImageView ivTripImage;

    Button btnPostTrip;

    FirebaseFirestore db;
    FirebaseAuth auth;

    Uri selectedImageUri = null;
    private static final int PICK_IMAGE_REQUEST = 101;

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
        // Date Picker
        // Date Picker
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

        // Select Image
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnCreateTrip.setOnClickListener(v -> saveTrip(false));
        btnPostTrip.setOnClickListener(v -> saveTrip(true));

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivTripImage.setImageURI(selectedImageUri);
        }
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

        if (name.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        Map<String, Object> trip = new HashMap<>();
        trip.put("name", name);
        trip.put("details", details);
        trip.put("days", days);
        trip.put("location", location);
        trip.put("startDate", startDate);
        trip.put("userId", userId);
        trip.put("email", email);
        trip.put("timestamp", System.currentTimeMillis());

        // ✅ Save to USER
        db.collection("users")
                .document(userId)
                .collection("trips")
                .add(trip)
                .addOnSuccessListener(doc -> {

                    if (isPost) {
                        postToFeed(trip);
                    } else {
                        Toast.makeText(this, "Trip created", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving trip", Toast.LENGTH_SHORT).show()
                );
    }

    // 🔥 CLEAN SEPARATION
    private void postToFeed(Map<String, Object> trip) {

        db.collection("feed")
                .add(trip)
                .addOnSuccessListener(feedDoc -> {
                    Toast.makeText(this, "Post created and posted", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Feed failed", Toast.LENGTH_SHORT).show()
                );
    }

    // 🔥 PROPER NAVIGATION
    private void navigateBack() {
        Intent intent = new Intent(this, TripListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}