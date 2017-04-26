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

/**
 * MainService handles main power intents
 * and requesting superuser
 */

public class MainService extends Service {
    public String TAG = this.getClass().getSimpleName();

    public static String ACTION_SU_NOT_GRANTED =
            "com.freshollie.headunitcontroller.action.SU_NOT_GRANTED";

    private SuperuserManager superuserManager;
    private NotificationHandler notificationHandler;

    private MediaMonitor mediaMonitor;
    private RoutineManager routineManager;

    private SharedPreferences sharedPreferences;

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
            if (!(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)
                    && !PowerUtil.isConnected(context))) {
                context.startService(startIntent);
            }
        }
    }

    /**
     * Set the test bindings for the input device
     */
    public void setTestKeyBindings() {
        DeviceKeyMapper keyMapper = new DeviceKeyMapper(getApplicationContext());

        keyMapper.clearAll();

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "com.apple.android.music",
                true,
                300);
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_ENTER)
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_1,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "com.freshollie.monkeyboarddabradio"
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_2,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "au.com.shiftyjelly.pocketcasts"
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "com.google.android.apps.maps"
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputManager.ACTION_START_DRIVING_MODE,
                true,
                1000
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputManager.ACTION_GO_HOME
        );
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputManager.ACTION_LAUNCH_VOICE_ASSIST,
                true,
                1000
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        );
        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_BACK),
                true,
                1000
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_RIGHT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_NEXT)
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_LEFT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_DPAD_UP)
        );

        keyMapper.setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_RIGHT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_TAB)
        );
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Started");

        sharedPreferences =
                getSharedPreferences(getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);

        superuserManager = SuperuserManager.getInstance();
        notificationHandler = new NotificationHandler(getApplicationContext());

        mediaMonitor = new MediaMonitor(getApplicationContext());
        if (hasListeningPermission()) {
            mediaMonitor.start();
        }

        routineManager = new RoutineManager(getApplicationContext());

        setTestKeyBindings();


        String [] args = new String[] {"logcat", "-v", "threadtime",
                "-f", Environment.getExternalStorageDirectory() + "logs/all.log",
                "-r", Integer.toString(100),
                "-n", Integer.toString(100)};

        try {
            Runtime.getRuntime().exec(args);
        } catch (IOException e) {
            e.printStackTrace();
        }

        startForeground(
                NotificationHandler.SERVICE_NOTIFICATION_ID,
                notificationHandler.notifyServiceStatus(getString(R.string.notify_running))
        );

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
                Log.v(TAG, "Has all permissions, running routine");

                StatusUtil.getInstance().setStatus("SU permission granted");

                notificationHandler.cancel(NotificationHandler.STATUS_NOTIFICATION_ID);

                switch(intent.getAction()) {
                    case Intent.ACTION_POWER_CONNECTED:
                        routineManager.onPowerConnected();
                        break;
                    case Intent.ACTION_POWER_DISCONNECTED:
                        routineManager.onPowerDisconnected();
                        break;
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
        stopForeground(true);
    }


}
