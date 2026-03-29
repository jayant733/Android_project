package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    ImageView ivProfile;
    EditText etName;
    Button btnSelectImage, btnSave;

    Uri selectedImageUri = null;
    private ActivityResultLauncher<String[]> imagePickerLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private String currentEmailKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfile = findViewById(R.id.ivProfile);
        etName = findViewById(R.id.etName);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);
        currentEmailKey = buildProfileKey(FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null);

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
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap == null) {
                        return;
                    }

                    Uri photoUri = saveBitmapToAppStorage(bitmap);
                    if (photoUri == null) {
                        Toast.makeText(this, "Could not save captured photo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedImageUri = photoUri;
                    ivProfile.setImageURI(photoUri);
                }
        );

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);

        etName.setText(prefs.getString(currentEmailKey + "_name", ""));

        String savedImage = prefs.getString(currentEmailKey + "_image", null);
        if (savedImage != null) {
            selectedImageUri = Uri.parse(savedImage);
            ivProfile.setImageURI(selectedImageUri);
        }

        btnSelectImage.setOnClickListener(v -> showPhotoOptions());
        ivProfile.setOnClickListener(v -> showPhotoOptions());

        // Save profile
        btnSave.setOnClickListener(v -> {

            String name = etName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(currentEmailKey + "_name", name);

            if (selectedImageUri != null) {
                editor.putString(currentEmailKey + "_image", selectedImageUri.toString());
            }

            editor.apply();

            Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private String buildProfileKey(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "default_profile";
        }
        return email.trim().toLowerCase(Locale.US).replace(".", "_");
    }

    private void showPhotoOptions() {
        String[] options = {"Take Photo", "Choose From Device"};

        new AlertDialog.Builder(this)
                .setTitle("Update Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        cameraLauncher.launch(null);
                    } else {
                        imagePickerLauncher.launch(new String[]{"image/*"});
                    }
                })
                .show();
    }

    private Uri saveBitmapToAppStorage(Bitmap bitmap) {
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "profile");
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }

        File imageFile = new File(directory, "profile_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            return null;
        }
    }
}
