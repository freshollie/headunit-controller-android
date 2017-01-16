package com.freshollie.headunitcontroller.input;

import android.content.ComponentName;
import android.content.Context;
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

/**
 * Created by freshollie on 1/1/17.
 */

public class DeviceInputManager {

    public static String TAG = DeviceInputManager.class.getSimpleName();

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

    private Context context;

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

                    stop();
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
                    if (actions[0] != null) {
                        handleActionRequest(actions[0], actions[1]);
                    } else {
                        actions = keyMapper.getKeyPressAction(id);
                        handleActionRequest(actions[0], actions[1]);
                    }
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

    public DeviceInputManager(Context serviceContext) {
        Log.v(TAG, "Created");
        context = serviceContext;

        packageManager = context.getPackageManager();
        mainLoopHandler = new Handler(context.getMainLooper());
        keyMapper = new DeviceKeyMapper(context);

        driver = new Driver(context);
        inputDevice = driver.getDevice();
    }

    public void start() {
        if (!inputDevice.isConnected()) {
            Log.v(TAG, "Starting driver");
            driver.start();
        }

        inputDevice.registerKeyListener(deviceKeyListener);
        inputDevice.registerConnectedListener(connectedListener);

    }

    public void stop() {
        driver.stop();

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
                    break;

                case ACTION_START_DRIVING_MODE:
                    startGoogleMapsDrivingMode();
                    break;

                case ACTION_LAUNCH_VOICE_ASSIST:
                    launchVoiceAssist();
                    break;

                case ACTION_GO_HOME:
                    goHome();
                    break;

                case ACTION_SEND_KEYEVENT:
                    if (extra != null) {
                        try {
                            sendKeyEvent(Integer.valueOf(extra));
                        } catch (NumberFormatException e) {
                            Log.v(TAG, "Somehow app launch got interpreted as a key press event");
                        }
                    }
                    break;
            }
        }
    }
    public void launchApp(String packageName) {
        Log.v(TAG, "Launching: " + packageName);
        Intent i = packageManager.getLaunchIntentForPackage(packageName);

        if (i != null) {
            context.startActivity(i);
        }

    }

    public void startGoogleMapsDrivingMode() {
        Log.v(TAG, "Launching driving mode");
        context.startActivity(
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
        context.startActivity(
                new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    public void launchVoiceAssist() {
        context.startActivity(
                new Intent("android.intent.action.VOICE_ASSIST")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    public void sendKeyEvent(int keyCode) {
        Log.v(TAG, "Sending key, " + String.valueOf(keyCode));
        SuperuserManager.getInstance().asyncExecute("input keyevent "+ String.valueOf(keyCode));
        context.sendBroadcast(new Intent(ACTION_SEND_KEYEVENT).putExtra("keyCode", keyCode));

    }
}
