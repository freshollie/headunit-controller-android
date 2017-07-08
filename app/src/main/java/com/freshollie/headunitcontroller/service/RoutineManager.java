package com.freshollie.headunitcontroller.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.rvalerio.fgchecker.AppChecker;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class RoutineManager {
    public String TAG = this.getClass().getSimpleName();
    public static int ATTACH_TIMEOUT = 3000; // Milliseconds

    public static int START_ROUTINE_RUN = 0;
    public static int STOP_ROUTINE_RUN = 1;
    private int lastState;

    private Context context;
    private SuperuserManager superuserManager;

    private SharedPreferences sharedPreferences;
    private MediaPlayer mediaPlayer;

    private Handler mainHandler;

    private boolean navigationStopped = true;

    private String STOP_MAPS_NAVIGATION =
            "com.freshollie.headunitcontroller.action.STOP_MAPS_NAVIGATION";

    private PendingIntent stopNavigationPendingIntent;

    private BroadcastReceiver stopNavigationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PowerUtil.isConnected(context)) {
                stopMapsNavigation();
            }
        }
    };

    private AppChecker mapsForegroundChecker;

    private Runnable mapsNotForegroundRunnable = new Runnable() {
        @Override
        public void run() {
            if (PowerUtil.isConnected(context)) {
                Log.v(TAG, "Maps not in foreground");
                setMapsForeground(false);
            }
        }
    };

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
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onTimedOut();
                        }
                    });
                }

                mainHandler.post(new Runnable() {
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

        mainHandler = new Handler(serviceContext.getMainLooper());

        deviceInputManager = new DeviceInputManager(context);

        stopNavigationPendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(STOP_MAPS_NAVIGATION),
                PendingIntent.FLAG_UPDATE_CURRENT);
        context.registerReceiver(stopNavigationReceiver, new IntentFilter(STOP_MAPS_NAVIGATION));
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
                            "-a org.broeuschmeul.android.gps.usb.provider" +
                            ".driver.usbgpsproviderservice.intent.action.START_GPS_PROVIDER"
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

    public void startMapsForegroundChecker() {
        Log.v(TAG, "Starting maps foreground checker");
        mapsForegroundChecker =
                new AppChecker()
                        .when("com.google.android.apps.maps", new AppChecker.Listener() {
                            @Override
                            public void onForeground(String process) {
                                if (PowerUtil.isConnected(context)) {
                                    if (!sharedPreferences.getBoolean(
                                            context.getString(R.string.MAPS_WAS_ON_SCREEN), false)) {
                                        Log.v(TAG, "Maps in foreground");
                                        mainHandler.removeCallbacks(mapsNotForegroundRunnable);
                                        setMapsForeground(true);
                                    }
                                }
                            }
                        }).other(new AppChecker.Listener() {
                    @Override
                    public void onForeground(String process) {
                        if (PowerUtil.isConnected(context)) {
                            if (sharedPreferences.getBoolean(context.getString(R.string.MAPS_WAS_ON_SCREEN), false)) {
                                mainHandler.postDelayed(mapsNotForegroundRunnable, 3000);
                            }
                        }
                    }
                });
        mapsForegroundChecker.start(context);
    }

    public void stopMapsForegroundChecker() {
        Log.v(TAG, "Stopping maps foreground checker");
        if (mapsForegroundChecker != null) {
            mapsForegroundChecker.stop();
            mapsForegroundChecker = null;
        }
    }

    public void setMapsForeground(boolean foreground) {
        sharedPreferences
                .edit()
                .putBoolean(context.getString(R.string.MAPS_WAS_ON_SCREEN), foreground)
                .apply();
    }

    public void startMapsDrivingMode() {
        context.startActivity(
                new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"))
                    .setPackage("com.google.android.apps.maps")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
    }

    public void scheduleStopMapsNavigation() {
        navigationStopped = false;
        long time = SystemClock.elapsedRealtime() + 300000;

        AlarmManager aMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, stopNavigationPendingIntent);
    }

    public void cancelStopMapsNavigation() {
        Log.v(TAG, "Cancelling stop navigation alarm");
        AlarmManager aMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        aMgr.cancel(stopNavigationPendingIntent);
    }

    public void stopMapsNavigation() {
        Log.v(TAG, "Stopping navigation");
        navigationStopped = true;
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

                if (navigationStopped &&
                        sharedPreferences.getBoolean(
                                context.getString(R.string.MAPS_WAS_ON_SCREEN), false) &&
                        sharedPreferences.getBoolean(
                                context.getString(R.string.SHOULD_START_DRIVING_MODE_KEY), false) &&
                        sharedPreferences.getBoolean(
                                context.getString(R.string.pref_launch_maps_key), true)) {
                    Log.v(TAG, "Launching maps");
                    StatusUtil.getInstance().setStatus("WakeUp: Launching Maps");
                    startMapsDrivingMode();
                } else {
                    /**
                     * if driving mode is not running then that means we shouldn't
                     * start driving mode next time until driving mode is relaunched
                     */
                    if (!sharedPreferences
                            .getBoolean(
                                    context.getString(R.string.DRIVING_MODE_RUNNING_KEY),
                                    false
                            )) {
                        sharedPreferences
                                .edit()
                                .putBoolean(
                                        context.getString(R.string.SHOULD_START_DRIVING_MODE_KEY),
                                        false
                                )
                                .apply();
                    }
                }

                if (!sharedPreferences
                        .getString(context.getString(R.string.pref_shell_wakeup_commands_key), "")
                        .isEmpty()) {
                    StatusUtil.getInstance().setStatus("WakeUp: Running shell commands");
                    superuserManager.asyncExecute(sharedPreferences
                            .getString(context.getString(R.string.pref_shell_wakeup_commands_key), "")
                    );
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

            if (sharedPreferences.getBoolean(context.getString(R.string.pref_stop_navigation_key), true)) {
                StatusUtil.getInstance().setStatus("Suspend: Scheduling maps to stop");
                scheduleStopMapsNavigation();
            }

            if (!sharedPreferences
                    .getString(context.getString(R.string.pref_shell_suspend_commands_key), "")
                    .isEmpty()) {
                StatusUtil.getInstance().setStatus("Suspend: Running shell commands");
                superuserManager.asyncExecute(sharedPreferences
                        .getString(context.getString(R.string.pref_shell_suspend_commands_key), "")
                );
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

        if (sharedPreferences.getBoolean(context.getString(R.string.pref_launch_maps_key), true)) {
            StatusUtil.getInstance().setStatus("Starting maps foreground checker");
            startMapsForegroundChecker();
        }

        cancelStopMapsNavigation();

        int delay = Integer.valueOf(
                sharedPreferences.getString(context.getString(R.string.pref_wake_up_delay_key),
                        "1000")
        );

        StatusUtil.getInstance().setStatus("WakeUp: Waiting " + delay + "ms");

        mainHandler.postDelayed(new Runnable() {
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

        StatusUtil.getInstance().setStatus("Stopping maps foreground checker");
        stopMapsForegroundChecker();

        runSuspendSequence();

    }

    public void stop() {
        deviceInputManager.stop();
        context.unregisterReceiver(stopNavigationReceiver);
    }
}
