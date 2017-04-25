package com.freshollie.headunitcontroller.service;


import android.content.Context;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.utils.PowerUtil;

public class MapsListenerService extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences =
                getSharedPreferences(getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);
        Log.v(TAG, "Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (statusBarNotification.getPackageName().equals("com.google.android.apps.maps") &&
                !sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_KEY), false) &&
                !statusBarNotification.isClearable()) {
            Log.v(TAG, "Driving mode enabled");
            setDrivingMode(true);
        }
    }

    public void setDrivingMode(Boolean mode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.DRIVING_MODE_KEY), mode);
        editor.apply();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (PowerUtil.isConnected(getApplicationContext()) &&
                notification.getPackageName().equals("com.google.android.apps.maps") &&
                !notification.isClearable()) {
            Log.v(TAG, "Driving mode disabled");
            setDrivingMode(false);
        }
    }

}