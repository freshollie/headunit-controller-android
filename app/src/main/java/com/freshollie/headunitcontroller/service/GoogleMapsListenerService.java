package com.freshollie.headunitcontroller.service;


import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.utils.PowerUtil;
import com.freshollie.headunitcontroller.utils.StatusUtil;
import com.rvalerio.fgchecker.AppChecker;

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
                !sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_KEY), false) &&
                !statusBarNotification.isClearable()) {
            Log.v(TAG, "Driving mode enabled");
            setDrivingMode(true);
        }
    }

    public void setDrivingMode(Boolean mode) {
        sharedPreferences.edit().putBoolean(getString(R.string.DRIVING_MODE_KEY), mode).apply();
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