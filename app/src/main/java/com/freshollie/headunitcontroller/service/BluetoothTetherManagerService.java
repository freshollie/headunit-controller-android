package com.freshollie.headunitcontroller.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.freshollie.headunitcontroller.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by freshollie on 29/05/17.
 *
 * Service to handle automatically connecting and disconnecting to a bluetooth PAN device when
 * wifi connection is lost or gained.
 *
 * Android doesn't automatically use wifi when both the bluetooth PAN and wifi are connected.
 */

public class BluetoothTetherManagerService extends Service {
    private static String TAG = ConnectionStateReceiver.class.getSimpleName();

    private int tetherConnectCalled = 0;
    private int tetherDisconnectCalled = 0;

    private boolean serviceConnected = false;
    private BluetoothProfile bluetoothProfile;
    private static String className = "android.bluetooth.BluetoothPan";

    private NetworkInfo.State lastNetworkState = NetworkInfo.State.DISCONNECTED;
    private int lastBluetoothState = BluetoothAdapter.STATE_OFF;


    /**
     * Used to connect the receive a proxy to the bluetooth PAN service.
     */
    private class PanServiceListener implements BluetoothProfile.ServiceListener {

        public PanServiceListener() {

        }

        @Override
        public void onServiceConnected(int i, BluetoothProfile proxy) {
            serviceConnected = true;
            bluetoothProfile = proxy;
            Log.v(TAG, "Service connected");
        }


        @Override
        public void onServiceDisconnected(int i) {
            serviceConnected = false;
            bluetoothProfile = null;
            Log.v(TAG, "Service disconnected");
        }
    }

    /**
     * Receiver used to receive when bluetooth or wifi states change
     */
    public static class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            // Only deal with intents if we have a bluetooth address set
            if (!PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_bluetooth_tether_address), "")
                    .isEmpty()) {
                context.startService(intent.setClass(context, BluetoothTetherManagerService.class));
            }
        }
    }

    @Override
    public void onCreate() {
        try {
            Class<?> classBluetoothPan = Class.forName(className);

            Constructor<?> ctor =
                    classBluetoothPan
                            .getDeclaredConstructor(
                                    Context.class,
                                    BluetoothProfile.ServiceListener.class
                            );

            ctor.setAccessible(true);
            ctor.newInstance(getApplicationContext(), new PanServiceListener());
        } catch (Exception e) {
            Log.e(TAG, "Unable to reflect android.bluetooth.BluetoothPan", e);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (serviceConnected) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        Log.v(TAG, "Bluetooth turned on");
                        new Handler(getMainLooper()).postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        checkStates();
                                    }
                                },
                                5000
                        );
                    }
                    break;
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    NetworkInfo networkInfo =
                            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.getState() != lastNetworkState) {
                        Log.v(TAG, "Wifi network state changed");
                        checkStates();
                    }

                    lastNetworkState = networkInfo.getState();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private synchronized void checkStates() {

        boolean wifiConnected = false;

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        String deviceAddress = sharedPreferences.getString(
                getString(R.string.pref_bluetooth_tether_address),
                ""
        );

        BluetoothDevice device = null;
        try {
            device = BluetoothAdapter
                    .getDefaultAdapter()
                    .getRemoteDevice(deviceAddress);
        } catch (IllegalArgumentException ignored) {}

        if (device != null && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network[] networks = connManager.getAllNetworks();

            // Check for wifi connection
            if (networks != null && networks.length > 0) {
                for (Network network : networks) {
                    NetworkInfo ntkInfo = connManager.getNetworkInfo(network);

                    if (ntkInfo.getType() == ConnectivityManager.TYPE_WIFI && ntkInfo.isConnectedOrConnecting()) {
                        wifiConnected = true;
                        Log.v(TAG, "We are connected to a wifi network");
                    }
                }
            }

            if (!wifiConnected && tetherConnectCalled < 1) {
                // We are not connected on wifi and not connected on bluetooth so
                // connect to bluetooth
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                    final BluetoothDevice finalDevice = device;
                    tetherConnectCalled++;

                    new Handler(getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (lastNetworkState != NetworkInfo.State.CONNECTED &&
                                    lastNetworkState != NetworkInfo.State.CONNECTING) {
                                Log.v(TAG, "No wifi, so connecting to tether");
                                changeBluetoothTether(true, finalDevice);
                            }
                        }
                    }, 2000);
                    if (tetherConnectCalled > 0) {
                        new Handler(getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                tetherConnectCalled = 0;
                            }
                        }, 5000);
                    }
                }
            } else if (wifiConnected && tetherDisconnectCalled < 1) {
                // We are connected to wifi but still connected to bluetooth so
                // disconnect from bluetooth
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                    tetherDisconnectCalled++;
                    Log.v(TAG, "Wifi connected, so disconnecting from tether");
                    changeBluetoothTether(false, device);

                    if (tetherDisconnectCalled > 0) {

                        new Handler(getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                tetherDisconnectCalled = 0;
                            }
                        }, 5000);
                    }
                }
            }
        } else if (!deviceAddress.isEmpty() && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            // The previously saved devices has unpaired
            sharedPreferences
                    .edit()
                    .putString(getString(R.string.pref_bluetooth_tether_address), "")
                    .apply();
        }
    }

    private void changeBluetoothTether(final boolean connect,
                                       final BluetoothDevice device) {
        if (serviceConnected) {
            try {
                if (connect) {
                    // android.bluetooth.BluetoothPan.connect
                    Method connectMethod =
                            bluetoothProfile
                                    .getClass()
                                    .getDeclaredMethod("connect", BluetoothDevice.class);

                    if (!((Boolean) connectMethod.invoke(bluetoothProfile, device))) {
                        Log.e(TAG, "Unable to start connection");
                    }
                } else {
                    // android.bluetooth.BluetoothPan.disconnect
                    Method disconnectMethod =
                            bluetoothProfile
                                    .getClass()
                                    .getDeclaredMethod("disconnect", BluetoothDevice.class);

                    if (!((Boolean) disconnectMethod.invoke(bluetoothProfile, device))) {
                        Log.e(TAG, "Unable to stop connection");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to reflect android.bluetooth.BluetoothPan", e);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
