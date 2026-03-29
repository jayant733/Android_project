package com.example.tripsync.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class TripReminderService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We are not binding this service to any activity
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Toast.makeText(this, "Trip Reminder Service Started!", Toast.LENGTH_SHORT).show();


        new Handler().postDelayed(this::stopSelf, 5000);


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Trip Reminder Service Stopped!", Toast.LENGTH_SHORT).show();
    }
}
