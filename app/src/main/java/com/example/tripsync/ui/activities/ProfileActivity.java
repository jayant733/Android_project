package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;

public class ProfileActivity extends AppCompatActivity {

    ImageView ivProfile;
    EditText etName;
    Button btnSelectImage, btnSave;

    Uri selectedImageUri = null;
    private static final int PICK_IMAGE_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfile = findViewById(R.id.ivProfile);
        etName = findViewById(R.id.etName);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);

        // Load saved name
        etName.setText(prefs.getString("user_name", ""));

        // Load saved image
        String savedImage = prefs.getString("profile_image", null);
        if (savedImage != null) {
            ivProfile.setImageURI(Uri.parse(savedImage));
        }

        // Select image from gallery
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Save profile
        btnSave.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_name", name);

            if (selectedImageUri != null) {
                editor.putString("profile_image", selectedImageUri.toString());
            }

            editor.apply();

            Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            selectedImageUri = data.getData();
            ivProfile.setImageURI(selectedImageUri);
        }
    }
}