package com.freshollie.headunitcontroller.services.controllers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.services.input.DeviceInputManager;
import com.freshollie.headunitcontroller.util.Logger;
import com.freshollie.headunitcontroller.util.SuperuserManager;

/**
 * Created by freshollie on 03.12.17.
 */

public class DriversController {
    private static final String TAG = DriversController.class.getSimpleName();

    private static final String USB_GPS_PACKAGE = "org.broeuschmeul.android.gps.usb.provider";
    private static final String USB_GPS_SERVICE =
            USB_GPS_PACKAGE + ".driver.USBGpsProviderService";
    private static final String START_USB_SERVICE_ACTION =
            USB_GPS_PACKAGE + ".action.START_GPS_PROVIDER";

    private static final String AUTOBRIGHT_SERVICE_START_COMMAND =
            "am startservice " +
                    "-a android.intent.action.MAIN " +
                    "-n com.autobright.kevinforeman.autobright/.AutoBrightService;" +

                    "am run " +
                    "-a android.intent.action.MAIN " +
                    "-n com.autobright.kevinforeman.autobright/.AutoBright";

    private static final int ATTACH_TIMEOUT = 3000; // Milliseconds

    private final Intent startUsbGpsServiceIntent;

    private final Context context;
    private final Handler mainThread;
    private final SharedPreferences sharedPreferences;
    private final UsbManager usbManager;
    private final DeviceInputManager deviceInputManager;
    private final SuperuserManager superuserManager;

    private interface OnAllDevicesAttachedListener {
        void onAllAttached();
        void onTimedOut();
    }

    public DriversController(Context serviceContext) {
        context = serviceContext;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        mainThread = new Handler(context.getMainLooper());
        deviceInputManager = new DeviceInputManager(context);

        startUsbGpsServiceIntent = new Intent();
        startUsbGpsServiceIntent.setComponent(new ComponentName(USB_GPS_PACKAGE, USB_GPS_SERVICE));
        startUsbGpsServiceIntent.setAction(START_USB_SERVICE_ACTION);

        superuserManager = SuperuserManager.getInstance();
    }

    private void registerOnAllAttachedListener(final OnAllDevicesAttachedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = SystemClock.currentThreadTimeMillis();

                final int allDevices = Integer.valueOf(sharedPreferences.getString(
                        context.getString(R.string.pref_num_devices_key),
                        "3"
                ));

                while (usbManager.getDeviceList().size() < allDevices
                        && (SystemClock.currentThreadTimeMillis() - startTime) < ATTACH_TIMEOUT) {

                }

                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        if (usbManager.getDeviceList().size() < allDevices) {
                            listener.onTimedOut();
                        }
                        listener.onAllAttached();
                    }
                });
            }
        }).start();
    }

    public void startInputService() {
        deviceInputManager.start();
    }

    private void launchGpsService() {
        context.startService(startUsbGpsServiceIntent);
    }

    private void launchBrightnessControllerService() {
        Logger.log(TAG, "StartUp: Starting brightness controller");

        // Autobright is not an exported service :(
        if (superuserManager.hasPermission()) {
            superuserManager.asyncExecute(AUTOBRIGHT_SERVICE_START_COMMAND);
        }
    }

    public void onStartup() {
        if (sharedPreferences.getBoolean(
                context.getString(R.string.pref_launch_autobright_key), true)) {
            Logger.log(TAG, "StartUp: Starting Autobright");
            launchBrightnessControllerService();
        }

        Logger.log(TAG, "StartUp: Waiting for devices");

        registerOnAllAttachedListener(new OnAllDevicesAttachedListener() {
            @Override
            public void onAllAttached() {
                Logger.log(TAG, "StartUp: All devices attached");

                if (sharedPreferences.getBoolean(
                        context.getString(R.string.pref_launch_gps_key),
                        false)) {
                    Logger.log(TAG, "StartUp: Launching GPS service");
                    launchGpsService();
                }

                if (sharedPreferences.getBoolean(
                        context.getString(R.string.pref_input_service_enabled_key),
                        true)) {
                    Logger.log(TAG, "StartUp: Starting input");
                    startInputService();
                }
            }

            @Override
            public void onTimedOut() {
                Logger.log(TAG, "StartUp: Attach wait timed out");
            }
        });
    }

    public void onSuspend() {
        deviceInputManager.stop();
    }

    public void onPowerConnected() {

    }

    public void onPowerDisconnected() {

    }

    public void destroy() {
        deviceInputManager.destroy();
    }
}
