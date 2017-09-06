package com.freshollie.headunitcontroller.service;

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
import android.view.KeyEvent;
import android.widget.Toast;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.input.DeviceInputManager;
import com.freshollie.headunitcontroller.input.DeviceKeyMapper;
import com.freshollie.headunitcontroller.utils.NotificationHandler;
import com.freshollie.headunitcontroller.utils.PowerUtil;
import com.freshollie.headunitcontroller.utils.StatusUtil;
import com.freshollie.headunitcontroller.utils.SuperuserManager;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

import java.io.IOException;
import java.util.Arrays;

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

    private SuperuserManager superuserManager;
    private NotificationHandler notificationHandler;

    private MediaMonitor mediaMonitor;
    private RoutineManager routineManager;

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
            Log.v("kek", intent.getAction());
            if (!(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)
                    && !PowerUtil.isConnected(context))) {
                context.startService(startIntent);
            }
        }
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Started");

        superuserManager = SuperuserManager.getInstance();
        notificationHandler = new NotificationHandler(getApplicationContext());

        mediaMonitor = new MediaMonitor(getApplicationContext());
        if (hasListeningPermission()) {
            mediaMonitor.start();
        }

        routineManager = new RoutineManager(getApplicationContext());


        // Record logs
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

    public void setGlobalScreenOrientation(String orientationPref) {
        String autoEnabled = "settings put system accelerometer_rotation ";
        String orientationCommand = "settings put system user_rotation ";

        if (Integer.valueOf(orientationPref) == 0) {
            autoEnabled += "1";
        } else {
            autoEnabled += "0";
        }

        orientationCommand += String.valueOf(Integer.valueOf(orientationPref) - 1);

        if (superuserManager.hasPermission()) {
            SuperuserManager.getInstance().asyncExecute(orientationCommand);
            SuperuserManager.getInstance().asyncExecute(autoEnabled);
        }
    }

    public void informNoListeningPermission() {
        startActivity(
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        stopWithStatus(getString(R.string.notify_no_notification_listen_permission));
    }

    public void informNoUsageStatsPermission() {
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
        Log.v(TAG, "Received run intent: " + intent.getAction());
        
        if (intent.getAction().equals(ACTION_SU_NOT_GRANTED)) {
            StatusUtil.getInstance().setStatus("Superuser not granted");
            stopWithStatus(getString(R.string.notify_su_not_granted_closing));
            return START_NOT_STICKY;
        }

        if (!hasListeningPermission()) {
            Log.v(TAG, "No listening permission");
            informNoListeningPermission();
            return START_NOT_STICKY;
        }

        if (!hasUsageStatsPermission()) {
            Log.v(TAG, "Notifying no usage permission");
            informNoUsageStatsPermission();

        } else if (!superuserManager.hasPermission()) {
            Log.v(TAG, "No superuser permission, requesting");

            StatusUtil.getInstance().setStatus("Requesting SU");

            superuserManager.request(new SuperuserManager.permissionListener() {
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
                if (intent.getAction().equals(ACTION_START_INPUT_SERVICE)) {
                    routineManager.startInputService();
                } else {
                    Log.v(TAG, "Has all permissions, running routine");

                    StatusUtil.getInstance().setStatus("SU permission granted");

                    notificationHandler.cancel(NotificationHandler.STATUS_NOTIFICATION_ID);

                    // Listen for screen orientation changes
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .registerOnSharedPreferenceChangeListener(this);

                    // Set the correct orientation
                    setGlobalScreenOrientation(
                            PreferenceManager
                                    .getDefaultSharedPreferences(this)
                                    .getString(
                                            getString(R.string.pref_screen_orientation_key),
                                            "4"
                                    )
                    );

                    switch (intent.getAction()) {
                        case Intent.ACTION_POWER_CONNECTED:
                            routineManager.onPowerConnected();
                            break;

                        case Intent.ACTION_BOOT_COMPLETED:
                            if (PowerUtil.isConnected(this)) {
                                routineManager.onPowerConnected();
                            } else {
                                routineManager.onPowerDisconnected();
                            }
                            break;

                        case Intent.ACTION_POWER_DISCONNECTED:
                            routineManager.onPowerDisconnected();
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

    public void stopWithStatus(String status){
        Log.v(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStatus(status);
        stopSelf();
    }

    public void stopWithStatusAndAction(String status, PendingIntent action) {
        Log.v(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStatus(status, action);
        stopSelf();
    }

    @Override
    public void onDestroy(){
        Log.v(TAG, "Stopping");
        mediaMonitor.stop();
        routineManager.stop();
        // Listen for screen orientation changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        stopForeground(true);
    }


}
