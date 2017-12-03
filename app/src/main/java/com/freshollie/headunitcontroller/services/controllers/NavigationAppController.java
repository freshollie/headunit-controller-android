package com.freshollie.headunitcontroller.services.controllers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.util.Logger;
import com.freshollie.headunitcontroller.util.PowerUtil;
import com.freshollie.headunitcontroller.util.SuperuserManager;
import com.rvalerio.fgchecker.AppChecker;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by freshollie on 03.12.17.
 */

public class NavigationAppController {
    private static final String TAG = NavigationAppController.class.getSimpleName();

    // 5 minutes
    private static final int DELAY_BEFORE_STOP_MAPS_NAVIGATION_MS = 300000;
    private static final String STOP_MAPS_NAVIGATION =
            "com.freshollie.headunitcontroller.action.STOP_MAPS_NAVIGATION";
    private final BroadcastReceiver stopNavigationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PowerUtil.isConnected(context)) {
                stopMapsNavigation();
            }
        }
    };

    public static final String GOOGLE_MAPS_PACKAGE_ID = "com.google.android.apps.maps";
    private static final String GOOGLE_MAPS_SERVICE_STOP_COMMAND =
            "am stopservice " +
                    "-n com.google.android.apps.maps/" +
                    "com.google.android.apps.gmm.navigation.service.base.NavigationService";

    private boolean navigationStopped = true;

    private final Context context;
    private final Handler mainThread;
    private final SharedPreferences sharedPreferences;

    private final AlarmManager alarmManager;
    private final SuperuserManager superuserManager;

    private PendingIntent stopNavigationPendingIntent;

    private final AppChecker mapsForegroundChecker = new AppChecker()
            .when(GOOGLE_MAPS_PACKAGE_ID, new AppChecker.Listener() {
                @Override
                public void onForeground(String process) {
                    if (PowerUtil.isConnected(context)) {
                        if (!sharedPreferences.getBoolean(
                                context.getString(R.string.maps_was_on_screen_key), false)) {
                            Log.d(TAG, "Maps in foreground");
                            mainThread.removeCallbacks(mapsNotForegroundRunnable);
                            recordMapsForeground(true);
                        }
                    }
                }
            }).other(new AppChecker.Listener() {
                @Override
                public void onForeground(String process) {
                    if (PowerUtil.isConnected(context)) {
                        if (sharedPreferences.getBoolean(
                                context.getString(R.string.maps_was_on_screen_key),
                                false)) {
                            mainThread.postDelayed(mapsNotForegroundRunnable, 3000);
                        }
                    }
                }
            });

    private final Runnable mapsNotForegroundRunnable = new Runnable() {
        @Override
        public void run() {
            if (PowerUtil.isConnected(context)) {
                Log.d(TAG, "Maps not in foreground");
                recordMapsForeground(false);
            }
        }
    };

    public NavigationAppController(Context serviceContext) {
        context = serviceContext;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mainThread = new Handler(serviceContext.getMainLooper());

        alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        superuserManager = SuperuserManager.getInstance();

        stopNavigationPendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(STOP_MAPS_NAVIGATION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        context.registerReceiver(stopNavigationReceiver, new IntentFilter(STOP_MAPS_NAVIGATION));
    }

    private void startMapsForegroundChecker() {
        mapsForegroundChecker.start(context);
    }

    private void stopMapsForegroundChecker() {;
        mapsForegroundChecker.stop();
    }

    private void recordMapsForeground(boolean foreground) {
        sharedPreferences
                .edit()
                .putBoolean(context.getString(R.string.maps_was_on_screen_key), foreground)
                .apply();
    }

    private void startMapsDrivingMode() {
        context.startActivity(
                new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"))
                        .setPackage("com.google.android.apps.maps")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    private void scheduleStopMapsNavigation() {
        navigationStopped = false;
        long time = SystemClock.elapsedRealtime() + DELAY_BEFORE_STOP_MAPS_NAVIGATION_MS;

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, stopNavigationPendingIntent);
    }

    private void cancelStopMapsNavigation() {
        Log.d(TAG, "Cancelling stop navigation alarm");
        alarmManager.cancel(stopNavigationPendingIntent);
    }

    private void stopMapsNavigation() {
        Log.d(TAG, "Stopping navigation");
        navigationStopped = true;
        superuserManager.asyncExecute(GOOGLE_MAPS_SERVICE_STOP_COMMAND);
    }

    public void onStartup() {
        if (navigationStopped &&
                sharedPreferences.getBoolean(
                        context.getString(R.string.maps_was_on_screen_key),
                        false) &&
                sharedPreferences.getBoolean(
                        context.getString(R.string.should_start_driving_mode_key),
                        false) &&
                sharedPreferences.getBoolean(
                        context.getString(R.string.pref_launch_maps_key),
                        true)) {
            Logger.log(TAG, "startup: Launching Maps");
            startMapsDrivingMode();
        } else {
                    /*
                    if driving mode is not running then that means we shouldn't
                    start driving mode next time until driving mode is relaunched
                    */
            if (!sharedPreferences
                    .getBoolean(
                            context.getString(R.string.DRIVING_MODE_RUNNING_KEY),
                            false
                    )) {
                sharedPreferences
                        .edit()
                        .putBoolean(
                                context.getString(R.string.should_start_driving_mode_key),
                                false
                        )
                        .apply();
            }
        }
    }

    public void onSuspend() {
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_stop_navigation_key), true)) {
            Logger.log(TAG, "Suspend: Scheduling maps to stop");
            scheduleStopMapsNavigation();
        }
    }

    public void onPowerConnected() {
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_launch_maps_key), true)) {
            Logger.log(TAG, "Starting maps foreground checker");
            startMapsForegroundChecker();
        }

        cancelStopMapsNavigation();
    }

    public void onPowerDisconnected() {
        Logger.log(TAG, "Stopping maps foreground checker");
        stopMapsForegroundChecker();
    }

    public void destroy() {
        context.unregisterReceiver(stopNavigationReceiver);
    }
}
