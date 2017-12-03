package com.freshollie.headunitcontroller.services;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.services.controllers.NavigationAppController;
import com.freshollie.headunitcontroller.util.PowerUtil;
import com.freshollie.headunitcontroller.util.Logger;

public class GoogleMapsListenerService extends NotificationListenerService {
    private static final String TAG = GoogleMapsListenerService.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG, "Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean isDrivingNotification(StatusBarNotification notification) {
        return NavigationAppController.GOOGLE_MAPS_PACKAGE_ID
                .equals(notification.getPackageName()) &&
                !notification.isClearable();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        // Google maps has posted a notification and its a driving notification
        // and it wasn't previously posted
        if (isDrivingNotification(notification) &&
                !sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_RUNNING_KEY), false)) {
            Logger.log(TAG, "Driving mode enabled");
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
                .putBoolean(getString(R.string.should_start_driving_mode_key), value)
                .apply();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (isDrivingNotification(notification)) {
            Logger.log(TAG, "Driving mode disabled");
            setDrivingMode(false);
            if (PowerUtil.isConnected(getApplicationContext())) {
                setShouldStartDrivingMode(false);
            }
        }
    }
}