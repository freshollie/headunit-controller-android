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
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.input.DeviceInputService;
import com.freshollie.headunitcontroller.input.DeviceKeyMapper;
import com.freshollie.headunitcontroller.utils.NotificationHandler;
import com.freshollie.headunitcontroller.utils.SuperuserManager;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

/**
 * StartupService handles main power intents
 * and requesting superuser
 */

public class StartupService extends Service {
    public String TAG = this.getClass().getSimpleName();
    public static String ACTION_SU_NOT_GRANTED =
            "com.freshollie.headunitcontroller.action.SU_NOT_GRANTED";

    private SuperuserManager superuserManager;
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
            Intent startIntent = new Intent(context, StartupService.class);
            startIntent.setAction(intent.getAction()); // Let the service know why it was started

            context.startService(startIntent);
        }
    }

    /**
     * Set the test bindings for the input device
     */
    public void setTestKeybindings() {
        DeviceKeyMapper keyMapper = new DeviceKeyMapper(getApplicationContext());

        // Wheel binds

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputService.ACTION_LAUNCH_APP,
                "com.apple.android.music",
                true,
                500);
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputService.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_ENTER)
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_1,
                DeviceInputService.ACTION_LAUNCH_APP,
                "com.freshollie.radioapp"
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_2,
                DeviceInputService.ACTION_LAUNCH_APP,
                "au.com.shiftyjelly.pocketcasts"
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputService.ACTION_LAUNCH_APP,
                "com.google.android.apps.maps"
        );
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputService.ACTION_START_DRIVING_MODE,
                true,
                1000
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputService.ACTION_GO_HOME
        );
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputService.ACTION_LAUNCH_VOICE_ASSIST,
                true,
                1000
        );

        // Ring Binds

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputService.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        );
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputService.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_BACK),
                true,
                500
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_RIGHT,
                DeviceInputService.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_NEXT)
        );

        // Wheel Binds

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_LEFT,
                DeviceInputService.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_DPAD_UP)
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_RIGHT,
                DeviceInputService.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_TAB)
        );
    }

    @Override
    public void onCreate() {
        sharedPreferences =
                getSharedPreferences(getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);
        superuserManager = SuperuserManager.getInstance();
        notificationHandler = new NotificationHandler(getApplicationContext());

        Log.v(TAG, "Started");

        setTestKeybindings();

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
        Log.v(TAG, "Received start intent: " + intent.getAction());
        if (intent.getAction().equals(ACTION_SU_NOT_GRANTED)) {
            stopWithStatus(getString(R.string.notify_su_not_granted_closing));
            return START_NOT_STICKY;
        }

        if (!hasListeningPermission()) {
            Log.v(TAG, "No listening permission");
            informNoListeningPermission();
            return START_NOT_STICKY;
        }

        if (!superuserManager.hasPermission()) {
            Log.v(TAG, "No superuser permission, requesting");
            if (!hasUsageStatsPermission()) {
                Log.v(TAG, "Notifiying no usage permission");
                informNoUsageStatsPermission();
            }
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
                Log.v(TAG, "Has all permissions, launching");
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
        stopService(new Intent(this, RoutineService.class));

    }


}
