package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class PastTripsActivity extends AppCompatActivity {

    ListView listPastTrips;
    TextView tvPastTripCount;
    ArrayList<String> pastTrips;
    ArrayList<String> pastTripDocIds;
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_trips);

        listPastTrips = findViewById(R.id.listPastTrips);
        tvPastTripCount = findViewById(R.id.tvPastTripCount);
        findViewById(R.id.btnBackPastTrips).setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        pastTrips = new ArrayList<>();
        pastTripDocIds = new ArrayList<>();

        listPastTrips.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, TripDetailsActivity.class);
            intent.putExtra("trip_doc_id", pastTripDocIds.get(position));
            startActivity(intent);
        });

        loadPastTrips();
    }

    private void loadPastTrips() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .orderBy("startDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pastTrips.clear();
                    pastTripDocIds.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"COMPLETED".equals(status)) {
                            continue;
                        }

                        String name = doc.getString("name");
                        String destination = doc.getString("location");
                        String startDate = doc.getString("startDate");

                        pastTrips.add(
                                (name != null ? name : "Completed Trip") +
                                        " - " +
                                        (destination != null ? destination : "Unknown") +
                                        "\nCompleted • " +
                                        (startDate != null ? startDate : "Unknown date")
                        );
                        pastTripDocIds.add(doc.getId());
                    }

                    tvPastTripCount.setText("Completed Trips: " + pastTrips.size());
                    listPastTrips.setAdapter(new PastTripAdapter());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load past trips", Toast.LENGTH_SHORT).show()
                );
    }

    private class PastTripAdapter extends ArrayAdapter<String> {

        PastTripAdapter() {
            super(PastTripsActivity.this, 0, pastTrips);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_trip, parent, false);
            }

            TextView name = convertView.findViewById(R.id.tvTripItemName);
            ImageView menu = convertView.findViewById(R.id.ivMenu);
            name.setText(pastTrips.get(position));
            menu.setVisibility(View.GONE);
            return convertView;
        }
    }
}
