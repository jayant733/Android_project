package com.example.tripsync.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tripsync.R;
import com.example.tripsync.data.model.TripModel;

import java.util.ArrayList;

public class TripAdapter extends ArrayAdapter<TripModel> {

    public TripAdapter(@NonNull Context context, ArrayList<TripModel> trips) {
        super(context, 0, trips);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.trip_item, parent, false);
        }

        TripModel trip = getItem(position);

        TextView tvTitle = convertView.findViewById(R.id.tvTripTitle);
        TextView tvSub = convertView.findViewById(R.id.tvTripSubtitle);

        tvTitle.setText(trip.getTitle());
        tvSub.setText(trip.getSubtitle());

        // 🔥 CLICK LOCATION → OPEN MAPS
        tvSub.setOnClickListener(v -> {

            String location = trip.getSubtitle();

            try {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    getContext().startActivity(mapIntent);
                } else {
                    // fallback to browser
                    Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location));
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                    getContext().startActivity(webIntent);
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "Unable to open Maps", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }
}