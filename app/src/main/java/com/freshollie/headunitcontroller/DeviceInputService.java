package com.freshollie.headunitcontroller;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.freshollie.shuttlexpressdriver.Driver;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

import java.util.ArrayList;

/**
 * Created by freshollie on 1/1/17.
 */

public class DeviceInputService extends Service {

    public static String TAG = DeviceInputService.class.getSimpleName();

    public static String ACTION_LAUNCH_APP =
            "com.freshollie.headunitcontroller.action.LAUNCH_APP";
    public static String ACTION_SEND_KEYEVENT =
            "com.freshollie.headunitcontroller.action.SEND_KEYEVENT";
    public static String ACTION_START_DRIVING_MODE =
            "com.freshollie.headunitcontroller.action.START_DRIVING_MODE";
    public static String ACTION_GO_HOME =
            "com.freshollie.headunitcontroller.action.GO_HOME";
    public static String ACTION_LAUNCH_VOICE_ASSIST =
            "com.freshollie.headunitcontroller.action.LAUNCH_VOICE_ASSIST";

    private Driver driver;
    private ShuttleXpressDevice inputDevice;

    private PackageManager packageManager;

    private Handler mainLoopHandler;

    private long[] buttonHoldDelays = new long[] {
            2000,
            2000,
            2000,
            2000,
            2000
    };
    private Runnable[] buttonHoldRunnables = new Runnable[5];

    private long ringRightHoldDelay = 2000;
    private Runnable ringRightHoldRunnable;

    private long ringLeftHoldDelay = 2000;
    private Runnable ringLeftHoldRunnable;

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

                }
            };

    private ShuttleXpressDevice.ButtonListener deviceButtonListener = new ShuttleXpressDevice.ButtonListener() {
        @Override
        public void onDown(final int id) {
            if (buttonHoldRunnables[id] != null) {
                mainLoopHandler.removeCallbacks(buttonHoldRunnables[id]);
            }

            buttonHoldRunnables[id] = new Runnable() {
                @Override
                public void run() {
                    switch(id) {
                        case 0
                    }
                    buttonHoldRunnables[id] = null;
                }
            };
            mainLoopHandler.postDelayed(buttonHoldRunnables[id], buttonHoldDelays[id]);
        }

        @Override
        public void onUp(int id) {
            if (buttonHoldRunnables[id] != null) {
                mainLoopHandler.removeCallbacks(buttonHoldRunnables[id]);
                buttonHoldRunnables[id] = null;

            } else {

            }

        }
    };

    private ShuttleXpressDevice.ClickWheelListener clickWheelListener = new ShuttleXpressDevice.ClickWheelListener() {
        @Override
        public void onRight() {

        }

        @Override
        public void onLeft() {

        }
    };

    private ShuttleXpressDevice.RingListener ringListener = new ShuttleXpressDevice.RingListener() {
        @Override
        public void onRight() {

        }

        @Override
        public void onLeft() {

        }

        @Override
        public void onMiddle() {

        }
    };

    public void onCreate() {
        packageManager = getPackageManager();
        mainLoopHandler = new Handler(getMainLooper());

        driver = new Driver(getApplicationContext());
        inputDevice = driver.getDevice();

        if (!inputDevice.isConnected()) {
            driver.start();
        }

        inputDevice.registerButtonListener(deviceButtonListener);
        inputDevice.registerClickWheelListener(clickWheelListener);
        inputDevice.registerRingListener(ringListener);
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
        inputDevice.unregisterButtonListener(deviceButtonListener);
        inputDevice.unregisterClickWheelListener(clickWheelListener);
        inputDevice.unregisterRingListener(ringListener);
        inputDevice.unregisterConnectedListener(connectedListener);
    }

    public void launchApp(String packageName) {
        Intent i = packageManager.getLaunchIntentForPackage(packageName);

        if (i != null) {
            startActivity(i);
        }

    }

    public void startGoogleMapsDrivingMode() {
        startActivity(
                new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"))
                        .setComponent(
                                new ComponentName(
                                        "com.google.android.apps.maps",
                                        "com.google.android.maps.MapsActivity"
                                )
                        )
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
                new Intent(Intent.ACTION_VOICE_COMMAND)
        );
    }

    public void sendKey(int key) {
        SuperUserManager.getInstance().execute("input keyevent "+ String.valueOf(key));
    }
}
