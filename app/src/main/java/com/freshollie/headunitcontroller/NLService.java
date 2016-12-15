package com.freshollie.headunitcontroller;


import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NLService extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences =
                getSharedPreferences(getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);
        Log.v(TAG, "Running");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        Log.v(TAG, "Posted " + notification.getPackageName());
        if (notification.getPackageName() == "com.google.android.apps.maps" &&
                !sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_KEY), false)
                ) {
            Log.v(TAG, "Driving mode enabled");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.DRIVING_MODE_KEY), true);
            editor.apply();
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (PowerUtil.isConnected(getApplicationContext()) &&
                notification.getPackageName() == "com.google.android.apps.maps") {
            Log.v(TAG, "Driving mode disabled");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.DRIVING_MODE_KEY), false);
            editor.apply();
        }
    }

}