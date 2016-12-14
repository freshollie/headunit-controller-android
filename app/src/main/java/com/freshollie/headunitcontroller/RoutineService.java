package com.freshollie.headunitcontroller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class RoutineService extends Service {
    public String TAG = "RoutineService";
    public static int ALL_DEVICES = 0;
    public static int ATTACH_TIMEOUT = 3000; // Milliseconds

    public interface OnAllDevicesAttachedListener{
        void onAllAttached();
    }

    @Override
    public void onCreate() {

    }

    public void waitForAttached(final OnAllDevicesAttachedListener listener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                UsbManager usbManager =
                        (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);

                long startTime = SystemClock.currentThreadTimeMillis();

                while (usbManager.getDeviceList().size() <ALL_DEVICES){
                    if ((SystemClock.currentThreadTimeMillis() - startTime) > ATTACH_TIMEOUT) {
                        // Wait timed out running routing anyway
                        break;
                    }
                }

                listener.onAllAttached();
            }
        }).start();
    }

    public void runStartRoutine() {

    }

    public void runStopSequence() {

    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.v(TAG, "Routine starting");
        Log.v(TAG, intent.getAction());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
