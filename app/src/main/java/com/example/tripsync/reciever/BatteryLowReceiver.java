package com.example.tripsync.reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BatteryLowReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "⚠ Battery Low! Please charge your device.", Toast.LENGTH_LONG).show();
    }
}

