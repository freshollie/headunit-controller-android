package com.freshollie.headunitcontroller.service;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.utils.PowerUtil;

public class GoogleMapsListenerService extends NotificationListenerService {
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Log.v(TAG, "Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (statusBarNotification.getPackageName().equals("com.google.android.apps.maps") &&
                !sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_RUNNING_KEY), false) &&
                !statusBarNotification.isClearable()) {
            Log.v(TAG, "Driving mode enabled");
            setDrivingMode(true);
            setShouldStartDrivingMode(true);
        }
    }

    public void setDrivingMode(boolean mode) {
        sharedPreferences.edit().putBoolean(getString(R.string.DRIVING_MODE_RUNNING_KEY), mode).apply();
    }

    public void setShouldStartDrivingMode(boolean value) {
        sharedPreferences
                .edit()
                .putBoolean(getString(R.string.SHOULD_START_DRIVING_MODE_KEY), value)
                .apply();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (notification.getPackageName().equals("com.google.android.apps.maps") &&
                !notification.isClearable()) {
            Log.v(TAG, "Driving mode disabled");
            setDrivingMode(false);
            if (PowerUtil.isConnected(getApplicationContext())) {
                setShouldStartDrivingMode(false);
            }
        }
    }

}