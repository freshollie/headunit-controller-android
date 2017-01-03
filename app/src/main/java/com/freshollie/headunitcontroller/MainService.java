package com.freshollie.headunitcontroller;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * MainService handles main power intents
 * and requesting superuser
 */

public class MainService extends Service {
    public static String TAG = "MainService";
    public static String ACTION_SU_NOT_GRANTED =
            "com.freshollie.headunitcontroller.action.SU_NOT_GRANTED";

    private SuperUserManager superUserManager;
    private NotificationHandler notificationHandler;

    private SharedPreferences sharedPreferences;

    public static class PowerAndBootReceiver extends BroadcastReceiver {
        /**
         * Receives all power and boot commands
         * and sends the action to the service
         *
         * @param context application context
         * @param intent the intent to start
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startIntent = new Intent(context, MainService.class);
            startIntent.setAction(intent.getAction());
            // Let the service know why it was started

            context.startService(startIntent);
        }
    }

    public void setTestSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(getString(R.string.BUTTON_PRESS_EVENT_KEY, 0),
                DeviceInputService.ACTION_SEND_KEYEVENT);
        editor.putInt(getString(R.string.BUTTON_PRESS_EVENT_KEY, 0),
                KeyEvent.KEYCODE_ENTER);

        editor.putString(getString(R.string.BUTTON_HOLD_EVENT_KEY, 0),
                DeviceInputService.ACTION_LAUNCH_APP);
        editor.putString(getString(R.string.BUTTON_HOLD_LAUNCH_APP_PACKAGE_KEY, 0),
                "com.apple.android.music");


        editor.putString(getString(R.string.BUTTON_PRESS_EVENT_KEY, 0),
                DeviceInputService.ACTION_LAUNCH_APP);
        editor.putString(getString(R.string.BUTTON_PRESS_LAUNCH_APP_PACKAGE_KEY, 0),
                "com.freshollie.monkeyboardradio");

        editor.putString(getString(R.string.BUTTON_HOLD_EVENT_KEY, 0),
                DeviceInputService.ACTION_LAUNCH_APP);
        editor.putString(getString(R.string.BUTTON_HOLD_LAUNCH_APP_PACKAGE_KEY, 0),
                "com.apple.android.music");
    }

    @Override
    public void onCreate() {
        sharedPreferences =
                getSharedPreferences(getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);

        superUserManager = SuperUserManager.getInstance();
        notificationHandler = new NotificationHandler(getApplicationContext());

        Log.v(TAG, "Started");

        if (hasListeningPermission()) {
            startMediaMonitor();
        }

    }

    public void startMediaMonitor() {
        startService(new Intent(getApplicationContext(), MediaMonitoringService.class));
    }

    public void informNoListeningPermission() {
        startActivity(
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        stopWithStatus(getString(R.string.notify_no_notification_listen_permission));
    }

    public void informNoUsageStatsPermission() {
        notificationHandler.notifyStatus(getString(R.string.notify_no_usage_stats_permission));
    }

    /**
     * Checks for notification listening permission
     */
    public boolean hasListeningPermission() {
        String notificationListenerString =
                Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");

        return !(notificationListenerString == null ||
                !notificationListenerString.contains(getPackageName()));
    }

    /**
     * Checks for usage stats permission for this app
     * @return
     */
    public boolean hasUsageStatsPermission() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent.getAction().equals(ACTION_SU_NOT_GRANTED)) {
            stopWithStatus(getString(R.string.notify_su_not_granted_closing));
            return START_NOT_STICKY;
        }

        if (!hasListeningPermission()) {
            informNoListeningPermission();
            return START_NOT_STICKY;
        }

        if (!superUserManager.hasPermission()) {
            if (!hasUsageStatsPermission()) {
                informNoUsageStatsPermission();
            }
            superUserManager.request(new SuperUserManager.OnPermissionListener() {
                @Override
                public void onGranted() {
                    Log.v(TAG, "SU permission granted");
                    startService(intent);
                }

                @Override
                public void onDenied() {
                    Log.v(TAG, "SU permission denied");
                    intent.setAction(ACTION_SU_NOT_GRANTED);
                    startService(intent);
                }
            });

        } else {
            if (intent.getAction() != null) {
                startService(
                        new Intent(getApplicationContext(), RoutineService.class)
                                .setAction(intent.getAction())
                );
            }

        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void stopWithStatus(String status){
        Log.v(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStopWithStatus(status);
        stopSelf();
    }

    public void stopWithStatusAndAction(String status, PendingIntent action) {
        Log.v(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStopWithStatusAndAction(status, action);
        stopSelf();
    }

    @Override
    public void onDestroy(){
        stopService(new Intent(this, MediaMonitoringService.class));
        stopService(new Intent(this, RoutineService.class));

    }


}
