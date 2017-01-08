package com.freshollie.headunitcontroller.input;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

import com.freshollie.headunitcontroller.utils.SuperuserManager;
import com.freshollie.shuttlexpressdriver.Driver;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

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

    private Runnable[] buttonHoldRunnables = new Runnable[5];
    private Runnable ringRightHoldRunnable;
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
                    buttonHoldRunnables[id] = null;
                    String[] actions = keyMapper.getButtonHoldAction(id);
                    handleActionRequest(actions[0], actions[1]);
                }
            };
            mainLoopHandler.postDelayed(buttonHoldRunnables[id], keyMapper.getButtonHoldDelay(id));
        }

        @Override
        public void onUp(int id) {
            if (buttonHoldRunnables[id] != null) {
                mainLoopHandler.removeCallbacks(buttonHoldRunnables[id]);
                buttonHoldRunnables[id] = null;

                String[] actions = keyMapper.getButtonPressAction(id);
                handleActionRequest(actions[0], actions[1]);
            }

        }
    };

    private ShuttleXpressDevice.ClickWheelListener clickWheelListener = new ShuttleXpressDevice.ClickWheelListener() {
        @Override
        public void onRight() {
            String[] actions = keyMapper.getWheelAction(ShuttleXpressDevice.ACTION_RIGHT);
            handleActionRequest(actions[0], actions[1]);
        }

        @Override
        public void onLeft() {
            String[] actions = keyMapper.getWheelAction(ShuttleXpressDevice.ACTION_LEFT);
            handleActionRequest(actions[0], actions[1]);
        }
    };

    private ShuttleXpressDevice.RingListener ringListener = new ShuttleXpressDevice.RingListener() {
        @Override
        public void onRight() {
            if (ringRightHoldRunnable != null) {
                mainLoopHandler.removeCallbacks(ringRightHoldRunnable);
            }

            // This function will run if the button is held for the
            // correct amount of time
            ringRightHoldRunnable = new Runnable() {
                @Override
                public void run() {
                    ringRightHoldRunnable = null;

                    String[] actions =
                            keyMapper.getRingHoldAction(ShuttleXpressDevice.POSITION_RIGHT);
                    handleActionRequest(actions[0], actions[1]);

                }
            };

            mainLoopHandler.postDelayed(ringRightHoldRunnable,
                    keyMapper.getRingHoldDelay(ShuttleXpressDevice.POSITION_RIGHT));

        }

        @Override
        public void onLeft() {
            if (ringLeftHoldRunnable != null) {
                mainLoopHandler.removeCallbacks(ringLeftHoldRunnable);
            }

            ringLeftHoldRunnable = new Runnable() {
                @Override
                public void run() {
                    ringLeftHoldRunnable = null;
                    
                    String[] actions =
                            keyMapper.getRingHoldAction(ShuttleXpressDevice.POSITION_LEFT);
                    handleActionRequest(actions[0], actions[1]);
                }
            };

            mainLoopHandler.postDelayed(ringLeftHoldRunnable,
                    keyMapper.getRingHoldDelay(ShuttleXpressDevice.POSITION_LEFT));

        }

        @Override
        public void onMiddle() {
            // If either of these runables are not null, then it means the hold
            // function did not complete, so run a press function for that side
            if (ringLeftHoldRunnable != null) {
                mainLoopHandler.removeCallbacks(ringLeftHoldRunnable);
                ringLeftHoldRunnable = null;

                String[] actions = keyMapper.getRingPressAction(ShuttleXpressDevice.POSITION_LEFT);
                handleActionRequest(actions[0], actions[1]);

            } else if (ringRightHoldRunnable != null){
                mainLoopHandler.removeCallbacks(ringRightHoldRunnable);
                ringRightHoldRunnable = null;

                String[] actions = keyMapper.getRingPressAction(ShuttleXpressDevice.POSITION_RIGHT);
                handleActionRequest(actions[0], actions[1]);
            } else {
                // If we every have an action for middle.
            }
        }
    };

    public void onCreate() {
        packageManager = getPackageManager();
        mainLoopHandler = new Handler(getMainLooper());
        keyMapper = new DeviceKeyMapper(getApplicationContext());

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

    public void handleActionRequest(String action, String extra) {
        switch (action) {
            case ACTION_LAUNCH_APP:
                launchApp(extra);

            case ACTION_START_DRIVING_MODE:
                startGoogleMapsDrivingMode();

            case ACTION_LAUNCH_VOICE_ASSIST:
                launchVoiceAssist();

            case ACTION_GO_HOME:
                goHome();

            case ACTION_SEND_KEYEVENT:
                if (extra != null) {
                    sendKeyEvent(Integer.valueOf(extra));
                }
        }
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

    public void sendKeyEvent(int key) {
        SuperuserManager.getInstance().execute("input keyevent "+ String.valueOf(key));
    }
}
