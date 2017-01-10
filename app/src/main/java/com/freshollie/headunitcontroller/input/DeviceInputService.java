package com.freshollie.headunitcontroller.input;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import com.freshollie.headunitcontroller.utils.SuperuserManager;
import com.freshollie.shuttlexpressdriver.Driver;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

import java.util.HashMap;

/**
 * Created by freshollie on 1/1/17.
 */

public class DeviceInputService extends Service {

    public static String TAG = DeviceInputService.class.getSimpleName();

    public static final String ACTION_LAUNCH_APP =
            "com.freshollie.headunitcontroller.action.LAUNCH_APP";
    public static final String ACTION_SEND_KEYEVENT =
            "com.freshollie.headunitcontroller.action.SEND_KEYEVENT";
    public static final String ACTION_START_DRIVING_MODE =
            "com.freshollie.headunitcontroller.action.START_DRIVING_MODE";
    public static final String ACTION_GO_HOME =
            "com.freshollie.headunitcontroller.action.GO_HOME";
    public static final String ACTION_LAUNCH_VOICE_ASSIST =
            "com.freshollie.headunitcontroller.action.LAUNCH_VOICE_ASSIST";

    private Driver driver;
    private ShuttleXpressDevice inputDevice;

    private PackageManager packageManager;

    private DeviceKeyMapper keyMapper;

    private Handler mainLoopHandler;

    private SparseArray<Runnable> keyHoldRunnables = new SparseArray<>();

    private ShuttleXpressDevice.ConnectedListener connectedListener =
            new ShuttleXpressDevice.ConnectedListener() {
                @Override
                public void onConnected() {

                }

                /**
                 * When the device disconnects we might as well close this service
                 */
                @Override
                public void onDisconnected() {
                    stopSelf();
                }
            };

    private ShuttleXpressDevice.KeyListener deviceKeyListener = new ShuttleXpressDevice.KeyListener() {
        @Override
        public void onDown(final int id) {
            if (keyHoldRunnables.get(id, null) != null) {
                mainLoopHandler.removeCallbacks(keyHoldRunnables.get(id));
            }

            keyHoldRunnables.append(id, new Runnable() {
                @Override
                public void run() {
                    keyHoldRunnables.append(id, null);
                    String[] actions = keyMapper.getKeyHoldAction(id);
                    handleActionRequest(actions[0], actions[1]);
                }
            });
            mainLoopHandler.postDelayed(keyHoldRunnables.get(id), keyMapper.getKeyHoldDelay(id));
        }

        @Override
        public void onUp(int id) {
            if (keyHoldRunnables.get(id, null) != null) {
                mainLoopHandler.removeCallbacks(keyHoldRunnables.get(id));
                keyHoldRunnables.append(id, null);

                String[] actions = keyMapper.getKeyPressAction(id);
                handleActionRequest(actions[0], actions[1]);
            }
        }
    };

    public void onCreate() {
        Log.v(TAG, "Started");
        packageManager = getPackageManager();
        mainLoopHandler = new Handler(getMainLooper());
        keyMapper = new DeviceKeyMapper(getApplicationContext());

        driver = new Driver(getApplicationContext());
        inputDevice = driver.getDevice();

        if (!inputDevice.isConnected()) {
            Log.v(TAG, "Starting driver");
            driver.start();
        }

        inputDevice.registerKeyListener(deviceKeyListener);
        inputDevice.registerConnectedListener(connectedListener);

    }

    /**
     * Makes sure that when the service is killed it is restarted
     * when killed by the system.
     */
    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onDestroy() {
        inputDevice.unregisterKeyListener(deviceKeyListener);
        inputDevice.unregisterConnectedListener(connectedListener);
    }

    public void handleActionRequest(String action, String extra) {
        if (action != null) {
            switch (action) {
                case ACTION_LAUNCH_APP:
                    if (extra != null) {
                        launchApp(extra);
                    }
                    return;

                case ACTION_START_DRIVING_MODE:
                    startGoogleMapsDrivingMode();
                    return;

                case ACTION_LAUNCH_VOICE_ASSIST:
                    launchVoiceAssist();
                    return;

                case ACTION_GO_HOME:
                    goHome();
                    return;

                case ACTION_SEND_KEYEVENT:
                    if (extra != null) {
                        try {
                            sendKeyEvent(Integer.valueOf(extra));
                        } catch (NumberFormatException e) {
                            Log.v(TAG, "Somehow app launch got interpretted as a key press event");
                        }
                    }
                    return;
            }
        }
    }
    public void launchApp(String packageName) {
        Log.v(TAG, "Launching: " + packageName);
        Intent i = packageManager.getLaunchIntentForPackage(packageName);

        if (i != null) {
            startActivity(i);
        }

    }

    public void startGoogleMapsDrivingMode() {
        Log.v(TAG, "Launching driving mode");
        startActivity(
                new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"))
                        .setComponent(
                                new ComponentName(
                                        "com.google.android.apps.maps",
                                        "com.google.android.maps.MapsActivity"
                                )
                        )
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    public void goHome() {
        startActivity(
                new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    public void launchVoiceAssist() {
        startActivity(
                new Intent("android.intent.action.VOICE_ASSIST")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    public void sendKeyEvent(int key) {
        Log.v(TAG, "Sending key, " + String.valueOf(key));
        SuperuserManager.getInstance().asyncExecute("input keyevent "+ String.valueOf(key));
    }
}
