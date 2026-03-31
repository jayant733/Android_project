package com.example.tripsync.ui.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
        ImageView cover = convertView.findViewById(R.id.ivFeedCover);
        ImageView star1 = convertView.findViewById(R.id.star1);
        ImageView star2 = convertView.findViewById(R.id.star2);
        ImageView star3 = convertView.findViewById(R.id.star3);
        ImageView star4 = convertView.findViewById(R.id.star4);
        ImageView star5 = convertView.findViewById(R.id.star5);
        ImageView[] stars = new ImageView[]{star1, star2, star3, star4, star5};

        FeedPost post = feedPosts.get(position);
        title.setText(post.tripName + "\n" + post.location + "\n" + post.email);
        details.setText(post.details != null && !post.details.isEmpty() ? post.details : "Shared trip post");
        ratingText.setText("Avg Rating: " + String.format(Locale.US, "%.1f", post.averageRating) + " (" + post.ratingCount + ")");
        bindImage(cover, post.imageUri);
        renderStars(stars, Math.round(post.averageRating));

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
                    renderStars(stars, userRating.intValue());
                }
                setStarsEnabled(stars, false);
                setLockedStarClicks(stars);
            } else {
                setStarsEnabled(stars, true);
                bindRatingClicks(post, userRatingRef, ratingText, stars);
            }
        }).addOnFailureListener(e -> {
            setStarsEnabled(stars, true);
            bindRatingClicks(post, userRatingRef, ratingText, stars);
        });

        return convertView;
    }

    private void bindRatingClicks(FeedPost post,
                                  DocumentReference userRatingRef,
                                  TextView ratingText,
                                  ImageView[] stars) {
        for (int i = 0; i < stars.length; i++) {
            final int selectedRating = i + 1;
            stars[i].setOnClickListener(v -> saveRating(post, userRatingRef, selectedRating, ratingText, stars));
        }
    }

    private void setLockedStarClicks(ImageView[] stars) {
        for (ImageView star : stars) {
            star.setOnClickListener(v ->
                    Toast.makeText(context, "You already rated this post", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void saveRating(FeedPost post,
                            DocumentReference userRatingRef,
                            int selectedRating,
                            TextView ratingText,
                            ImageView[] stars) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        userRatingRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                setStarsEnabled(stars, false);
                setLockedStarClicks(stars);
                Toast.makeText(context, "You already rated this post", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> rating = new HashMap<>();
            rating.put("value", selectedRating);
            rating.put("userId", auth.getCurrentUser().getUid());
            rating.put("updatedAt", System.currentTimeMillis());

            userRatingRef.set(rating, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        renderStars(stars, selectedRating);
                        setStarsEnabled(stars, false);
                        setLockedStarClicks(stars);
                        recalculateAverage(post, ratingText);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Could not save rating", Toast.LENGTH_SHORT).show()
                    );
        }).addOnFailureListener(e ->
                Toast.makeText(context, "Could not save rating", Toast.LENGTH_SHORT).show()
        );
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
                        Long value = doc.getLong("value");
                        if (value == null) {
                            Double valueDouble = doc.getDouble("value");
                            if (valueDouble == null) {
                                continue;
                            }
                            total += valueDouble;
                        } else {
                            total += value;
                        }
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

    private void renderStars(ImageView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setImageResource(i < rating
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
        }
    }

    private void bindImage(ImageView imageView, String imageUri) {
        imageView.setImageURI(null);
        if (imageUri == null || imageUri.trim().isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }

        try {
            imageView.setImageURI(Uri.parse(imageUri));
        } catch (Exception e) {
            imageView.setImageDrawable(null);
        }
    }

    private void setStarsEnabled(ImageView[] stars, boolean enabled) {
        for (ImageView star : stars) {
            star.setEnabled(enabled);
            star.setClickable(true);
            star.setAlpha(enabled ? 1f : 0.7f);
        }
    }
}
