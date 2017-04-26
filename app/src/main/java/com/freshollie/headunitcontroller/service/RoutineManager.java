package com.freshollie.headunitcontroller.service;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.input.DeviceInputManager;
import com.freshollie.headunitcontroller.utils.PowerUtil;
import com.freshollie.headunitcontroller.utils.StatusUtil;
import com.freshollie.headunitcontroller.utils.SuperuserManager;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class RoutineManager {
    public String TAG = this.getClass().getSimpleName();
    public static int ATTACH_TIMEOUT = 3000; // Milliseconds

    public static String CONTEXT_USAGE_STATS_MANAGER = "usagestats";

    public static int START_ROUTINE_RUN = 0;
    public static int STOP_ROUTINE_RUN = 1;
    private int lastState;

    private Context context;
    private SuperuserManager superuserManager;

    private SharedPreferences sharedPreferences;
    private MediaPlayer mediaPlayer;

    private PowerManager.WakeLock wakeLock;

    private DeviceInputManager deviceInputManager;

    private interface OnAllDevicesAttachedListener {
        void onAllAttached();
        void onTimedOut();
    }

    public void registerOnAllAttachedListener(final OnAllDevicesAttachedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                long startTime = SystemClock.currentThreadTimeMillis();

                int allDevices = Integer.valueOf(sharedPreferences.getString(
                        context.getString(R.string.pref_num_devices_key),
                        "0"
                ));

                while (usbManager.getDeviceList().size() < allDevices
                    && (SystemClock.currentThreadTimeMillis() - startTime) < ATTACH_TIMEOUT) {

                }

                if (usbManager.getDeviceList().size() < allDevices) {
                    new Handler(context.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onTimedOut();
                        }
                    });
                }

                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onAllAttached();
                    }
                });
            }
        }).start();
    }

    public RoutineManager(Context serviceContext) {
        context = serviceContext;
        superuserManager = SuperuserManager.getInstance();
        Log.v(TAG, "Created");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        lastState = STOP_ROUTINE_RUN;
        deviceInputManager = new DeviceInputManager(context);
    }

    public void acquireWakeLock() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Headunit Controller");
        wakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (wakeLock != null) {
            try {
                wakeLock.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public void startBlankAudio() {
        stopBlankAudio();
        mediaPlayer = MediaPlayer.create(context, R.raw.blank);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        Log.v(TAG, "Blank audio started");
    }

    public void stopBlankAudio(){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.v(TAG, "Blank audio stopped");
        }
    }

    public void raiseVolume() {
        int volume = Integer.valueOf(sharedPreferences.getString(
                context.getString(R.string.pref_volume_level_key), "13")
        );

        Log.v(TAG, "Raising system volume to " + volume);

        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE))
                .setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }


    /**
     * Run generic play command on given audio app.
     *
     * If the app has a specific method to play, then that method will be run
     * @param packageName
     */
    public void playAudioApp(String packageName) {
        Log.v(TAG, "Playing " + packageName);

        StatusUtil.getInstance().setStatus("WakeUp: Playing " + packageName);

        switch (packageName) {
            case "com.apple.android.music":
                if (superuserManager.hasPermission()) {
                    superuserManager.asyncExecute(
                            "am startservice " +
                                    "-a 'com.apple.music.client.player.play_pause' " +
                                    "-n com.apple.android.music" +
                                    "/com.apple.android.svmediaplayer.player.MusicService");
                }
                break;

            case "com.freshollie.radioapp":
                if (superuserManager.hasPermission()) {
                    registerOnAllAttachedListener(new OnAllDevicesAttachedListener() {
                        @Override
                        public void onAllAttached() {
                            superuserManager.asyncExecute(
                                    "am startservice -n com.freshollie.radioapp/.radioservice");
                        }

                        @Override
                        public void onTimedOut() {

                        }
                    });
                }
                break;
            default:
                /*
                 * Default method is to send a playpause key to that app.
                 * However this does not work on some apps
                 */

                long eventTime = SystemClock.uptimeMillis();

                Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent downEvent = new KeyEvent(eventTime, eventTime,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                downIntent.setPackage(packageName);
                context.sendOrderedBroadcast(downIntent, null);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventTime, eventTime,
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                upIntent.setPackage(packageName);
                context.sendOrderedBroadcast(upIntent, null);
        }
    }

    public void playLastAudioSource() {
        String lastPlayingAudioApp =
                sharedPreferences.getString(context.getString(R.string.PLAYING_AUDIO_APP_KEY), "");

        if (!lastPlayingAudioApp.isEmpty()) {
            playAudioApp(lastPlayingAudioApp);
        }
    }

    public void launchGpsService() {
        Log.v(TAG, "Launching GPS service");

        if (superuserManager.hasPermission()) {
            superuserManager.asyncExecute(
                    "am startservice " +
                            "-a org.broeuschmeul.android.gps" +
                            ".usb.provider.nmea.intent.action.START_GPS_PROVIDER"
            );
        }
    }

    public void launchBrightnessControllerService() {
        Log.v(TAG, "Starting brightness controller");

        StatusUtil.getInstance().setStatus("WakeUp: Starting brightness controller");

        if (superuserManager.hasPermission()) {
            superuserManager.asyncExecute(
                    "am startservice " +
                            "-a android.intent.action.MAIN " +
                            "-n com.autobright.kevinforeman.autobright/.AutoBrightService"
            );

            superuserManager.asyncExecute(
                    "am run " +
                            "-a android.intent.action.MAIN " +
                            "-n com.autobright.kevinforeman.autobright/.AutoBright"
            );
        }
    }

    public void startInputService() {
        Log.v(TAG, "Starting input service");
        StatusUtil.getInstance().setStatus("WakeUp: Starting input");
        deviceInputManager.run();
    }

    public void launchMaps() {
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

    public boolean isMapsForeground() {
        return getForegroundPackageName().equals("com.google.android.apps.maps");
    }

    public void checkMapsForeground() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.v(TAG, "Checked foreground and found: " + getForegroundPackageName());

        StatusUtil.getInstance().setStatus("Foreground: " + getForegroundPackageName());
        if (isMapsForeground()) {
            Log.v(TAG, "Maps in foreground");
            editor.putBoolean(context.getString(R.string.LAUNCH_MAPS_KEY), true);
        } else {
            editor.putBoolean(context.getString(R.string.LAUNCH_MAPS_KEY), false);
        }
        editor.apply();
    }

    public void stopMapsNavigation() {
        superuserManager.asyncExecute(
                "am stopservice " +
                "-n com.google.android.apps.maps/" +
                        "com.google.android.apps.gmm.navigation.service.base.NavigationService");
    }

    public void runWakeUpRoutine() {
        Log.v(TAG, "Running wakeup routine");

        if (PowerUtil.isConnected(context)) {
            if (lastState != START_ROUTINE_RUN) {
                lastState = START_ROUTINE_RUN;

                if (sharedPreferences.getBoolean(
                        context.getString(R.string.pref_play_media_key), true)) {
                    StatusUtil.getInstance().setStatus("WakeUp: playing last media");
                    playLastAudioSource();
                }

                if (sharedPreferences.getBoolean(
                        context.getString(R.string.pref_launch_autobright_key), true)) {
                    StatusUtil.getInstance().setStatus("WakeUp: Starting Autobright");
                    launchBrightnessControllerService();
                }

                StatusUtil.getInstance().setStatus("WakeUp: Waiting for devices");

                registerOnAllAttachedListener(new OnAllDevicesAttachedListener() {
                    @Override
                    public void onAllAttached() {
                        Log.v(TAG, "All devices attached");
                        StatusUtil.getInstance().setStatus("WakeUp: Devices attached");

                        if (sharedPreferences.getBoolean(
                                context.getString(R.string.pref_launch_gps_key), false)) {
                            StatusUtil.getInstance().setStatus("WakeUp: Launching GPS");
                            launchGpsService();
                        }

                        if (sharedPreferences.getBoolean(
                                context.getString(R.string.pref_input_service_enabled_key), true)) {
                            StatusUtil.getInstance().setStatus("WakeUp: Starting Input");
                            startInputService();
                        }

                        Log.v(TAG, "Wakeup routine complete");
                        StatusUtil.getInstance().setStatus("WakeUp: Done");
                    }

                    @Override
                    public void onTimedOut() {
                        Log.v(TAG, "Timed out waiting for devices to connect");
                        StatusUtil.getInstance().setStatus("WakeUp: Wait timed out");
                    }
                });

                if (!sharedPreferences.getBoolean(
                        context.getString(R.string.pref_debug_enabled_key), true) &&
                        sharedPreferences.getBoolean(
                                context.getString(R.string.pref_set_volume_key), true)) {
                    Log.v(TAG, "Setting Volume");

                    StatusUtil.getInstance().setStatus("WakeUp: Setting Volume");
                    raiseVolume();
                }

                if (sharedPreferences.getBoolean(
                        context.getString(R.string.LAUNCH_MAPS_KEY), false) &&
                        sharedPreferences.getBoolean(
                                context.getString(R.string.DRIVING_MODE_KEY), false) &&
                        sharedPreferences.getBoolean(
                                context.getString(R.string.pref_launch_maps_key), true)) {

                    Log.v(TAG, "Launching maps");
                    StatusUtil.getInstance().setStatus("WakeUp: Launching Maps");
                    launchMaps();
                }

            } else {
                Log.v(TAG, "Aborting run routine as it has " +
                        "already been run once while power is connected");

                StatusUtil.getInstance().setStatus("WakeUp: Aborting, already run");
            }
        } else {
            StatusUtil.getInstance().setStatus("WakeUp: Aborting, no power");
            Log.v(TAG, "Power no longer connected, aborting");
        }
    }

    public void runSuspendSequence() {
        Log.v(TAG, "Running stop routine");

        if (lastState == START_ROUTINE_RUN) {
            lastState = STOP_ROUTINE_RUN;

            StatusUtil.getInstance().setStatus("Suspend");

            checkMapsForeground();

            if (sharedPreferences.getBoolean(context.getString(R.string.pref_stop_navigation_key), true)) {
                StatusUtil.getInstance().setStatus("Stopping navigation service");
                stopMapsNavigation();
            }

            StatusUtil.getInstance().setStatus("Suspend Complete");
        } else {
            StatusUtil.getInstance().setStatus("Suspend: Aborting, already run");
            Log.v(TAG, "Aborting, stop routine already run");
        }

    }

    public void onPowerConnected() {

        if (sharedPreferences.getBoolean(context.getString(R.string.pref_blank_audio_key), true)) {
            StatusUtil.getInstance().setStatus("Starting blank audio");
            startBlankAudio();
        }

        if (sharedPreferences.getBoolean(context.getString(R.string.pref_wakelock_key), true)) {
            StatusUtil.getInstance().setStatus("Acquiring wakelock");
            acquireWakeLock();
        }

        int delay = Integer.valueOf(
                sharedPreferences.getString(context.getString(R.string.pref_wake_up_delay_key),
                        "1000")
        );

        StatusUtil.getInstance().setStatus("WakeUp: Waiting " + delay + "ms");

        new Handler(context.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                runWakeUpRoutine();
            }
        }, delay);
    }

    public void onPowerDisconnected() {
        StatusUtil.getInstance().setStatus("Power disconnected");

        StatusUtil.getInstance().setStatus("Stopping blank audio");
        stopBlankAudio();

        StatusUtil.getInstance().setStatus("Release wakelock");
        releaseWakeLock();

        runSuspendSequence();
    }

    public String getForegroundPackageName(){
        String currentApp = "";

        //noinspection ResourceType
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(CONTEXT_USAGE_STATS_MANAGER);

        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 1000, time);
        // Gets the apps used in the last second

        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();

            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(),
                        usageStats);
            }

            if (!mySortedMap.isEmpty()) {
                // Get the top app
                currentApp = mySortedMap.get(
                        mySortedMap.lastKey()).getPackageName();
            }
        }

        return currentApp;
    };

    public void stop() {
        deviceInputManager.stop();
    }
}
