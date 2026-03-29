package com.example.tripsync.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tripsync.R;
import com.example.tripsync.data.model.FeedPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedAdapter extends ArrayAdapter<FeedPost> {

    private final Context context;
    private final List<FeedPost> feedPosts;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FeedAdapter(@NonNull Context context, @NonNull List<FeedPost> feedPosts) {
        super(context, 0, feedPosts);
        this.context = context;
        this.feedPosts = feedPosts;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
        }

        TextView title = convertView.findViewById(R.id.tvFeedTitle);
        TextView details = convertView.findViewById(R.id.tvFeedDetails);
        TextView ratingText = convertView.findViewById(R.id.tvFeedRating);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);

        FeedPost post = feedPosts.get(position);
        title.setText(post.tripName + "\n" + post.location + "\n" + post.email);
        details.setText(post.details != null && !post.details.isEmpty() ? post.details : "Shared trip post");
        ratingText.setText("Avg Rating: " + String.format(Locale.US, "%.1f", post.averageRating) + " (" + post.ratingCount + ")");
        ratingBar.setOnRatingBarChangeListener(null);
        ratingBar.setRating(post.averageRating);

        if (auth.getCurrentUser() == null) {
            return convertView;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        DocumentReference userRatingRef = db.collection("feed")
                .document(post.docId)
                .collection("ratings")
                .document(currentUserId);

        userRatingRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Double userRating = snapshot.getDouble("value");
                if (userRating != null) {
                    ratingBar.setRating(userRating.floatValue());
                }
            }
        });

        ratingBar.setOnRatingBarChangeListener((bar, value, fromUser) -> {
            if (!fromUser || auth.getCurrentUser() == null) {
                return;
            }

            Map<String, Object> rating = new HashMap<>();
            rating.put("value", (double) value);
            rating.put("userId", currentUserId);
            rating.put("updatedAt", System.currentTimeMillis());

            userRatingRef.set(rating, SetOptions.merge())
                    .addOnSuccessListener(unused -> recalculateAverage(post, ratingText))
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Could not save rating", Toast.LENGTH_SHORT).show()
                    );
        });

        return convertView;
    }

    private void recalculateAverage(FeedPost post, TextView ratingText) {
        db.collection("feed")
                .document(post.docId)
                .collection("ratings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double total = 0;
                    int count = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Double value = doc.getDouble("value");
                        if (value == null) {
                            continue;
                        }
                        total += value;
                        count++;
                    }

                    double average = count == 0 ? 0 : total / count;

                    Map<String, Object> aggregate = new HashMap<>();
                    aggregate.put("averageRating", average);
                    aggregate.put("ratingCount", count);

                    db.collection("feed")
                            .document(post.docId)
                            .set(aggregate, SetOptions.merge());

                    ratingText.setText("Avg Rating: " + String.format(Locale.US, "%.1f", average) + " (" + count + ")");
                    Toast.makeText(context, "You have rated successfully", Toast.LENGTH_SHORT).show();
                });
    }
}
