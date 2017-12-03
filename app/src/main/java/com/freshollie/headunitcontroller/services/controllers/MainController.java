package com.freshollie.headunitcontroller.services.controllers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.util.PowerUtil;
import com.freshollie.headunitcontroller.util.Logger;
import com.freshollie.headunitcontroller.util.SuperuserManager;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class MainController {
    public static final String TAG = MainController.class.getSimpleName();

    private static final int STATE_START_ROUTINE_RUN = 0;
    private static final int STATE_STOP_ROUTINE_RUN = 1;

    private int lastState;

    private Context context;
    private SharedPreferences sharedPreferences;
    private SuperuserManager superuserManager;
    private final PowerManager powerManager;

    private Handler mainThread;

    private PowerManager.WakeLock wakeLock;

    private PlaybackController playbackController;
    private NavigationAppController navigationAppController;
    private DriversController driversController;

    public MainController(Context serviceContext) {
        context = serviceContext;
        superuserManager = SuperuserManager.getInstance();

        playbackController = new PlaybackController(context);
        navigationAppController = new NavigationAppController(context);
        driversController = new DriversController(context);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        lastState = STATE_STOP_ROUTINE_RUN;

        mainThread = new Handler(serviceContext.getMainLooper());

        Log.d(TAG, "Created");
    }

    @SuppressLint("WakelockTimeout")
    private void acquireWakeLock() {
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            try {
                wakeLock.release();
            } catch (RuntimeException e) {
                Log.e(TAG, "Error releasing wakelock", e);
            }
            wakeLock = null;
        }
    }

    public DriversController getDriversController() {
        return driversController;
    }

    private void onStartup() {
        Logger.log(TAG, "Running wakeup routine");

        if (PowerUtil.isConnected(context)) {
            if (lastState != STATE_START_ROUTINE_RUN) {
                lastState = STATE_START_ROUTINE_RUN;

                playbackController.onStartup();
                driversController.onStartup();
                navigationAppController.onStartup();

                if (!sharedPreferences
                        .getString(context.getString(R.string.pref_shell_wakeup_commands_key), "")
                        .isEmpty()) {
                    Logger.log(TAG, "StartUp: Running shell commands");
                    superuserManager.asyncExecute(sharedPreferences
                            .getString(context.getString(R.string.pref_shell_wakeup_commands_key), "")
                    );
                }

            } else {
                Logger.log(TAG, "StartUp: Aborting, already run");
            }
        } else {
            Logger.log(TAG, "StartUp: Aborting, no power connected");
        }
    }

    public void onSuspend() {
        Log.d(TAG, "Running stop routine");

        if (lastState == STATE_START_ROUTINE_RUN) {
            lastState = STATE_STOP_ROUTINE_RUN;

            Logger.log(TAG, "Suspend started");

            playbackController.onSuspend();
            driversController.onSuspend();
            navigationAppController.onSuspend();


            if (!sharedPreferences
                    .getString(context.getString(R.string.pref_shell_suspend_commands_key), "")
                    .isEmpty()) {
                Logger.log(TAG, "Suspend: Running shell commands");
                superuserManager.asyncExecute(
                        sharedPreferences.getString(
                                context.getString(R.string.pref_shell_suspend_commands_key),
                                ""
                        )
                );
            }

            Logger.log(TAG, "Suspend Complete");
        } else {
            Logger.log(TAG, "Suspend: Aborting, already run");
        }
    }

    public void onPowerConnected() {
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_wakelock_key), true) && wakeLock == null) {
            Logger.log(TAG,"Acquiring wakelock");
            acquireWakeLock();
        }

        playbackController.onPowerConnected();
        navigationAppController.onPowerConnected();
        driversController.onPowerConnected();

        int delay = Integer.valueOf(
                sharedPreferences.getString(
                        context.getString(R.string.pref_wake_up_delay_key),
                        "1000"
                )
        );

        Logger.log(TAG, "StartUp: Waiting " + delay + "ms");

        mainThread.postDelayed(new Runnable() {
            public void run() {
                onStartup();
            }
        }, delay);
    }

    public void onPowerDisconnected() {
        Logger.log(TAG, "Power disconnected");

        if (wakeLock != null) {
            Logger.log(TAG, "Releasing wakelock");
            releaseWakeLock();
        }

        playbackController.onPowerDisconnected();
        navigationAppController.onPowerDisconnected();
        driversController.onPowerDisconnected();

        onSuspend();
    }

    public void destroy() {
        driversController.destroy();
        navigationAppController.destroy();
        playbackController.destroy();
    }
}
