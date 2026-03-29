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

import java.util.List;

public class FeedAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> feedTrips;

    public FeedAdapter(@NonNull Context context, @NonNull List<String> feedTrips) {
        super(context, 0, feedTrips);
        this.context = context;
        this.feedTrips = feedTrips;
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_feed, parent, false);
        }

        TextView title = convertView.findViewById(R.id.tvFeedTitle);
        TextView ratingText = convertView.findViewById(R.id.tvFeedRating);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);

        String tripTitle = feedTrips.get(position);
        title.setText(tripTitle);

        float rating = 3.5f + (position * 0.3f);
        ratingText.setText("⭐ " + rating);
        ratingBar.setRating(rating);

        ratingBar.setOnRatingBarChangeListener((bar, value, fromUser) -> {
            if (fromUser) {
                ratingText.setText("⭐ " + value);
                Toast.makeText(context,
                        "Rated " + value,
                        Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }
}