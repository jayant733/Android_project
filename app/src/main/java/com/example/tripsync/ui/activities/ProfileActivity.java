package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class ProfileActivity extends AppCompatActivity {

    ImageView ivProfile;
    EditText etName;
    Button btnSelectImage, btnSave;

    Uri selectedImageUri = null;
    private ActivityResultLauncher<String[]> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfile = findViewById(R.id.ivProfile);
        etName = findViewById(R.id.etName);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);

        imagePickerLauncher = registerForActivityResult(
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
                    ivProfile.setImageURI(uri);
                }
        );

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);

        // Load saved name
        etName.setText(prefs.getString("user_name", ""));

        // Load saved image
        String savedImage = prefs.getString("profile_image", null);
        if (savedImage != null) {
            ivProfile.setImageURI(Uri.parse(savedImage));
        }

        // Open the system file picker so emulator and real devices both show a proper image chooser.
        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        ivProfile.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));

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
}
