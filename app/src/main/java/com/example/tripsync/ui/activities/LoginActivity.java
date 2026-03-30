package com.example.tripsync.ui.activities;



import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.ui.common.EdgeToEdgeHelper;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvGoSignup;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EdgeToEdgeHelper.apply(this);

        etEmail = findViewById(R.id.tEmail);
        etPassword = findViewById(R.id.tPassword);
        btnLogin = findViewById(R.id.tnLogin);
        tvGoSignup = findViewById(R.id.vGoSignup);

        mAuth = FirebaseAuth.getInstance();

        // LOGIN
        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        saveSession(email);
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, TripListActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // GO TO SIGNUP
        tvGoSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // AUTO LOGIN
        if (mAuth.getCurrentUser() != null) {
            saveSession(mAuth.getCurrentUser().getEmail());
            startActivity(new Intent(this, TripListActivity.class));
            finish();
        }
    }

    private void saveSession(String email) {
        SharedPreferences prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        prefs.edit().putString("user_email", email).apply();
    }
}
