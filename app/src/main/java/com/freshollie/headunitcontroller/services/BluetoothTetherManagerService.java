package com.freshollie.headunitcontroller.services;

import android.annotation.SuppressLint;
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
import com.freshollie.headunitcontroller.util.Logger;

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
    private static final String TAG = BluetoothTetherManagerService.class.getSimpleName();
    private static final int DELAY_BEFORE_STARTING_CONNECTION = 5000;
    private static final int DELAY_AFTER_BLUETOOTH_TURNED_ON = 2000;

    // Keeps track of if this service has already tried to start before
    private static boolean failedToStarted = false;

    private boolean connectDelayRunning = false;

    private NetworkInfo.State lastNetworkState;

    private BluetoothPANInterface panInterface;
    private BluetoothAdapter bluetoothAdapter;

    private ConnectivityManager connectivityManager;

    private SharedPreferences sharedPreferences;
    private Handler mainThread;

    @Override
    public void onCreate() {
        // Don't keep trying to start if we have failed before
        if (!failedToStarted) {
            try {
                panInterface = new BluetoothPANInterface(this);
            } catch (Exception e) {
                Log.e(TAG, "Unable to initialise BluetoothPANInterface", e);
            }

            bluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();

            connectivityManager = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);

            if (bluetoothAdapter == null) {
                Log.e(TAG, "No bluetooth adapter found");
            }

            if (connectivityManager == null) {
                Log.e(TAG, "No connectivity manager found");
            }

            if (panInterface == null ||
                    bluetoothAdapter == null ||
                    connectivityManager == null) {
                failedToStarted = true;
                stopSelf();
                return;
            }

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            mainThread = new Handler(getMainLooper());
        }

        Log.d(TAG, "Created");
    }

    private boolean isTetherManagerReady() {
        return panInterface != null && panInterface.isReady();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (isTetherManagerReady()) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_OFF
                    );
                    if (state == BluetoothAdapter.STATE_ON) {
                        Log.d(TAG, "Bluetooth turned on");
                        mainThread.postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        manageConnections();
                                    }
                                },
                                DELAY_AFTER_BLUETOOTH_TURNED_ON
                        );
                    }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo networkInfo =
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo.getState() != lastNetworkState) {
                    Log.d(TAG, "Wifi network state changed");
                    manageConnections();
                }

                lastNetworkState = networkInfo.getState();
            }
        }
        return START_NOT_STICKY;
    }

    private boolean isWifiConnected() {
        Network[] networks = connectivityManager.getAllNetworks();

        if (networks != null && networks.length > 0) {
            for (Network network: networks) {
                NetworkInfo ntkInfo = connectivityManager.getNetworkInfo(network);

                if (ntkInfo != null &&
                        ntkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        ntkInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void startDelayedConnect(final BluetoothDevice device) {
        // Only run one of these every 5 seconds, and ignore all other requests
        if (connectDelayRunning) {
            return;
        }
        connectDelayRunning = true;

        mainThread.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isWifiConnected()) {
                    Logger.log(TAG, "No wifi, so connecting to tether");
                    panInterface.requestConnect(device);
                }
                connectDelayRunning = false;
            }
        }, DELAY_BEFORE_STARTING_CONNECTION);
    }

    /**
     * Check the state of the wifi or bluetooth connection and perform the relative operations
     * in order to ensure an established internet connection
     */
    private synchronized void manageConnections() {
        String selectedTetherDeviceName = sharedPreferences.getString(
                getString(R.string.pref_bluetooth_tether_address),
                ""
        );

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(selectedTetherDeviceName);

        if (bluetoothAdapter.isEnabled()) {
            if (device != null) {
                int bluetoothConnectionStatus = panInterface.getConnectionState(device);

                boolean bluetoothConnected =
                        bluetoothConnectionStatus != BluetoothProfile.STATE_DISCONNECTED &&
                                bluetoothConnectionStatus != BluetoothProfile.STATE_DISCONNECTING;

                if (!isWifiConnected()) {
                    if (!bluetoothConnected) {
                        // We are not connected to a wifi network, and not already connected to the
                        // bluetooth device, so start the connection now
                        startDelayedConnect(device);
                    }
                } else if (bluetoothConnected) {
                    // We are connected to wifi but still connected to bluetooth so
                    // disconnect from tethered bluetooth
                    panInterface.requestDisconnect(device);
                }
            } else if (!selectedTetherDeviceName.isEmpty()) {
                // The previously saved devices has unpaired, so remove it from our saved settings
                sharedPreferences
                        .edit()
                        .putString(getString(R.string.pref_bluetooth_tether_address), "")
                        .apply();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Used to start the receive a proxy to the bluetooth PAN service and provide
     * an interface for us to start to a device
     */
    private class BluetoothPANInterface implements BluetoothProfile.ServiceListener {
        private BluetoothProfile bluetoothPan;
        private Method connect;
        private Method disconnect;
        private Method getConnectionState;

        private Handler mainThread;

        private int DELAY_BETWEEN_CONNECTS = 2000;
        private int DELAY_BETWEEN_DISCONNECTS = 5000;

        private String PAN_CLASS_NAME = "android.bluetooth.BluetoothPan";
        private String PAN_CONNECT_METHOD_NAME = "start";
        private String PAN_DISCONNECT_METHOD_NAME = "disconnect";
        private String PAN_GET_CONNECTION_STATE_METHOD_NAME = "getConnectionState";

        private boolean connectDisabled;
        private boolean disconnectDisabled;

        private String TAG = BluetoothPANInterface.class.getSimpleName();

        BluetoothPANInterface(Context context) throws Exception {
            mainThread = new Handler(context.getMainLooper());

            Constructor<?> BluetoothPan = Class.forName(PAN_CLASS_NAME)
                    .getDeclaredConstructor(
                            Context.class,
                            BluetoothProfile.ServiceListener.class
                    );

            BluetoothPan.setAccessible(true);
            BluetoothPan.newInstance(context, this);
        }

        @SuppressLint("PrivateApi")
        @Override
        public void onServiceConnected(int i, BluetoothProfile proxy) {
            bluetoothPan = proxy;
            try {
                connect = bluetoothPan
                        .getClass()
                        .getDeclaredMethod(PAN_CONNECT_METHOD_NAME, BluetoothDevice.class);
                disconnect = bluetoothPan
                        .getClass()
                        .getDeclaredMethod(PAN_DISCONNECT_METHOD_NAME, BluetoothDevice.class);
                getConnectionState = bluetoothPan
                        .getClass()
                        .getDeclaredMethod(PAN_GET_CONNECTION_STATE_METHOD_NAME, BluetoothDevice.class);

                Log.d(TAG, "Bluetooth profile service connected");
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Unable to reflect android.bluetooth.BluetoothPan", e);
                connect = null;
                disconnect = null;
                getConnectionState = null;
                bluetoothPan = null;
            }
        }


        @Override
        public void onServiceDisconnected(int i) {
            if (bluetoothPan != null) {
                bluetoothPan = null;
                Log.d(TAG, "Bluetooth profile service disconnected");
            }
        }

        void requestConnect(BluetoothDevice device) {
            if (connectDisabled) {
                return;
            }
            Log.d(TAG, "Requesting start");

            try {
                if ((boolean) connect.invoke(bluetoothPan, device)) {
                    connectDisabled = true;

                    mainThread.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connectDisabled = false;
                        }
                    }, DELAY_BETWEEN_CONNECTS);
                } else {
                    Log.e(TAG, "Unable to start connection");
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to call device.requestConnect", e);
            }
        }

        void requestDisconnect(BluetoothDevice device) {
            if (disconnectDisabled) {
                return;
            }

            Log.d(TAG, "Requesting disconnect");

            try {
                if ((boolean) disconnect.invoke(bluetoothPan, device)) {
                    disconnectDisabled = true;

                    mainThread.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            disconnectDisabled = false;
                        }
                    }, DELAY_BETWEEN_DISCONNECTS);
                } else {
                    Log.e(TAG, "Unable to stop connection");
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to call device.requestDisconnect", e);
            }
        }

        int getConnectionState(BluetoothDevice device) {
            int state = BluetoothProfile.STATE_DISCONNECTED;

            if (getConnectionState != null) {
                try {
                    state = (int) getConnectionState.invoke(bluetoothPan, device);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to call getConnectionState", e);
                }
            }

            return state;
        }

        boolean isReady() {
            return bluetoothPan != null;
        }
    }

    /**
     * Receiver used to receive when bluetooth or wifi states change
     */
    public static class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction()) ||
                    BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                // Only deal with intents if we have a bluetooth address set
                if (!PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getString(context.getString(R.string.pref_bluetooth_tether_address), "")
                        .isEmpty()) {
                    if (!failedToStarted) {
                        // Forward the intents to the tether managing service
                        context.startService(
                                intent.setClass(context, BluetoothTetherManagerService.class)
                        );
                    }
                }
            }
        }
    }
}
