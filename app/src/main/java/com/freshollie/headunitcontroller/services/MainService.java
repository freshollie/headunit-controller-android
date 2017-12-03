package com.freshollie.headunitcontroller.services;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.util.NotificationHandler;
import com.freshollie.headunitcontroller.util.PowerUtil;
import com.freshollie.headunitcontroller.util.Logger;
import com.freshollie.headunitcontroller.util.SuperuserManager;
import com.freshollie.headunitcontroller.services.controllers.MainController;

import java.io.IOException;

/**
 * MainService handles main power intents
 * and requesting superuser
 */

public class MainService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
    public String TAG = this.getClass().getSimpleName();

    public static String ACTION_SU_NOT_GRANTED =
            "com.freshollie.headunitcontroller.action.SU_NOT_GRANTED";

    public static String ACTION_START_INPUT_SERVICE =
            "com.freshollie.headunitcontroller.action.ACTION_START_INPUT_SERVICE";

    private static String NOTIFICATION_LISTENER_SETTINGS_ACTION =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static String ENABLED_NOTIFICATION_LISTENERS_KEY =
            "enabled_notification_listeners";

    private SuperuserManager superuserManager;
    private NotificationHandler notificationHandler;

    private MediaMonitor mediaMonitor;
    private MainController mainController;
    private PackageManager packageManager;
    private AppOpsManager appOpsManager;

    private SharedPreferences sharedPreferences;


    @Override
    public void onCreate() {
        Log.d(TAG, "Started");

        superuserManager = SuperuserManager.getInstance();
        notificationHandler = new NotificationHandler(getApplicationContext());
        mainController = new MainController(getApplicationContext());

        mediaMonitor = new MediaMonitor(getApplicationContext());
        if (haveListeningPermission()) {
            mediaMonitor.start();
        }

        sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this);
        packageManager = getPackageManager();
        appOpsManager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);

        // Record more logs
        String [] args = new String[] {"logcat", "-v", "threadtime",
                "-f", Environment.getExternalStorageDirectory() + "logs/all.log",
                "-r", Integer.toString(100),
                "-n", Integer.toString(100)};

        try {
            Runtime.getRuntime().exec(args);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Ensure the service is not closed
        startForeground(
                NotificationHandler.SERVICE_NOTIFICATION_ID,
                notificationHandler.notifyServiceStatus(getString(R.string.notify_running))
        );
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preference) {
        if (preference.equals(getString(R.string.pref_screen_orientation_key))) {
            setGlobalScreenOrientation(sharedPreferences.getString(preference, "4"));
        }
    }

    private void setGlobalScreenOrientation(String orientationPref) {
        String autoEnabled = "settings put system accelerometer_rotation ";
        String orientationCommand = "settings put system user_rotation ";

        if (Integer.valueOf(orientationPref) == 0) {
            autoEnabled += "1";
        } else {
            autoEnabled += "0";
        }

        orientationCommand += String.valueOf(Integer.valueOf(orientationPref) - 1);

        if (superuserManager.hasPermission()) {
            superuserManager.asyncExecute(orientationCommand);
            superuserManager.asyncExecute(autoEnabled);
        }
    }

    private void informNoListeningPermission() {
        startActivity(
                new Intent(NOTIFICATION_LISTENER_SETTINGS_ACTION)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        stopWithStatus(getString(R.string.notify_no_notification_listen_permission));
    }

    private void informNoUsageStatsPermission() {
        notificationHandler.notifyStatus(
                getString(R.string.notify_no_usage_stats_permission),

                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
        );
    }

    /**
     * Checks for notification listening permission for this app
     */
    private boolean haveListeningPermission() {
        String enabledNotificationListeners =
                Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS_KEY);

        return !(enabledNotificationListeners == null ||
                !enabledNotificationListeners.contains(getPackageName()));
    }

    /**
     * Checks for usage stats permission for this app
     * @return
     */
    private boolean haveUsageStatsPermission() {
        try {
            ApplicationInfo applicationInfo =
                    packageManager.getApplicationInfo(getPackageName(), 0);
            int mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName
            );
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.d(TAG, "Received run intent: " + intent.getAction());
        
        if (ACTION_SU_NOT_GRANTED.equals(intent.getAction())) {
            Logger.log(TAG, "Superuser not granted");
            stopWithStatus(getString(R.string.notify_su_not_granted_closing));
            return START_NOT_STICKY;
        }

        if (!haveListeningPermission()) {
            Logger.log(TAG, "No listening permission");
            informNoListeningPermission();
            return START_NOT_STICKY;
        }

        if (!haveUsageStatsPermission()) {
            Logger.log(TAG, "Notifying no usage permission");
            informNoUsageStatsPermission();

        } else if (!superuserManager.hasPermission()) {
            Logger.log(TAG, "Requesting SU permission");

            superuserManager.request(new SuperuserManager.permissionListener() {
                @Override
                public void onGranted() {
                    Logger.log(TAG, "SU permission granted");
                    startService(intent);
                }

                @Override
                public void onDenied() {
                    Logger.log(TAG, "SU permission denied");
                    intent.setAction(ACTION_SU_NOT_GRANTED);
                    startService(intent);
                }
            });

        } else {
            if (intent.getAction() != null) {
                if (ACTION_START_INPUT_SERVICE.equals(intent.getAction())) {
                    mainController.getDriversController().startInputService();
                } else {
                    Logger.log(TAG, "Have all permissions, running routine");

                    notificationHandler.cancel(NotificationHandler.STATUS_NOTIFICATION_ID);

                    // Listen for screen orientation changes
                    sharedPreferences.registerOnSharedPreferenceChangeListener(this);

                    // Set the correct orientation
                    setGlobalScreenOrientation(
                            sharedPreferences.getString(
                                    getString(R.string.pref_screen_orientation_key),
                                    "4"
                            )
                    );

                    switch (intent.getAction()) {
                        case Intent.ACTION_POWER_CONNECTED:
                            mainController.onPowerConnected();
                            break;

                        case Intent.ACTION_BOOT_COMPLETED:
                            if (PowerUtil.isConnected(this)) {
                                mainController.onPowerConnected();
                            } else {
                                mainController.onPowerDisconnected();
                            }
                            break;

                        case Intent.ACTION_POWER_DISCONNECTED:
                            mainController.onPowerDisconnected();
                            break;
                    }
                }
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void stopWithStatus(String status){
        Log.d(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStatus(status);
        stopSelf();
    }

    private void stopWithStatusAndAction(String status, PendingIntent action) {
        Log.d(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStatus(status, action);
        stopSelf();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "Stopping");
        mediaMonitor.stop();
        mainController.destroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        stopForeground(true);
    }

    public static void start(Context context, String action) {
        context.startService(new Intent(context, MainService.class).setAction(action));
    }

    public static class PowerAndBootReceiver extends BroadcastReceiver {
        /**
         * Receives all power and boot commands
         * and sends the action to the service
         *
         * @param context application context
         * @param intent the intent to run
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startIntent = new Intent(context, MainService.class);
            startIntent.setAction(intent.getAction()); // Let the service know why it was started

            // Don't run if power is not actually connected
            if (!(Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())
                    && !PowerUtil.isConnected(context))) {
                context.startService(startIntent);
            }
        }
    }
}
