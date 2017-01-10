package com.freshollie.headunitcontroller.services;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.input.DeviceInputService;
import com.freshollie.headunitcontroller.utils.PowerUtil;
import com.freshollie.headunitcontroller.utils.SuperuserManager;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class RoutineService extends Service {
    public String TAG = this.getClass().getSimpleName();
    public static int ALL_DEVICES = 1;
    public static int ATTACH_TIMEOUT = 3000; // Milliseconds

    public static String CONTEXT_USAGE_STATS_MANAGER = "usagestats";

    public static int START_ROUTINE_RUN = 0;
    public static int STOP_ROUTINE_RUN = 1;
    private int lastState;

    private SharedPreferences sharedPreferences;
    private MediaPlayer mediaPlayer;

    private PowerManager.WakeLock wakeLock;

    private interface AllDevicesAttachedListener {
        void onAllAttached();
        void onTimedOut();
    }

    public void registerOnAllAttachedListener(final AllDevicesAttachedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                long startTime = SystemClock.currentThreadTimeMillis();

                while (usbManager.getDeviceList().size() < ALL_DEVICES
                    && (startTime - SystemClock.currentThreadTimeMillis()) < ATTACH_TIMEOUT) {

                }

                if (usbManager.getDeviceList().size() >= ALL_DEVICES) {
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onAllAttached();
                        }
                    });
                } else {
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onTimedOut();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "Routine service created");
        sharedPreferences = getSharedPreferences(
                getString(R.string.PREFERENCES_KEY),
                Context.MODE_PRIVATE
        );
        lastState = STOP_ROUTINE_RUN;

    }

    public void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.blank);
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
        Log.v(TAG, "Raising system volume");
        //((AudioManager) getSystemService(Context.AUDIO_SERVICE))
          //      .setStreamVolume(AudioManager.STREAM_MUSIC, 13, 0);
    }


    /**
     * Run generic play command on given audio app.
     *
     * If the app has a specific method to play, then that method will be run
     * @param packageName
     */
    public void playAudioApp(String packageName) {
        Log.v(TAG, "Playing " + packageName);

        switch (packageName) {
            case "com.apple.android.music":
                SuperuserManager superuserManager =
                        SuperuserManager.getInstance();

                if (superuserManager.hasPermission()) {
                    superuserManager.asyncExecute(
                            "am startservice " +
                                    "-a 'com.apple.music.client.player.play_pause' " +
                                    "-n com.apple.android.music" +
                                    "/com.apple.android.svmediaplayer.player.MusicService");
                }

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
                sendOrderedBroadcast(downIntent, null);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventTime, eventTime,
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                upIntent.setPackage(packageName);
                sendOrderedBroadcast(upIntent, null);
        }
    }

    public void playLastAudioSource() {
        String lastPlayingAudioApp =
                sharedPreferences.getString(getString(R.string.PLAYING_AUDIO_APP_KEY), null);

        if (lastPlayingAudioApp != null) {
            playAudioApp(lastPlayingAudioApp);
        }
    }

    public void launchGpsService() {
        SuperuserManager superuserManager =
                SuperuserManager.getInstance();

        if (superuserManager.hasPermission()) {
            superuserManager.asyncExecute(
                    "am startservice " +
                            "-a org.broeuschmeul.android.gps" +
                            ".usb.provider.nmea.intent.action.START_GPS_PROVIDER"
            );
        }
    }

    public void launchBrightnessControllerService() {
        SuperuserManager superuserManager =
                SuperuserManager.getInstance();

        if (superuserManager.hasPermission()) {
            superuserManager.asyncExecute(
                    "am startservice " +
                            "-a android.intent.action.MAIN " +
                            "-n com.autobright.kevinforeman.autobright/.AutoBrightService"
            );

            superuserManager.asyncExecute(
                    "am start " +
                            "-a android.intent.action.MAIN " +
                            "-n com.autobright.kevinforeman.autobright/.AutoBright"
            );
        }
    }

    public void startInputService() {
        Log.v(TAG, "Starting input service");
        startService(new Intent(getApplicationContext(), DeviceInputService.class));
    }

    public void launchMaps() {
        startActivity(
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

    public void checkMapsForeground() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.v(TAG, "Checked foreground and found: " + getForegroundPackageName());
        if (getForegroundPackageName().equals("com.google.android.apps.maps")) {
            Log.v(TAG, "Maps in foreground");
            editor.putBoolean(getString(R.string.LAUNCH_MAPS_KEY), true);
        } else {
            editor.putBoolean(getString(R.string.LAUNCH_MAPS_KEY), false);
        }
        editor.apply();
    }

    public void runStartRoutine() {
        Log.v(TAG, "Running start routine");
        if (PowerUtil.isConnected(getApplicationContext())) {
            if (lastState != START_ROUTINE_RUN) {
                lastState = START_ROUTINE_RUN;
                playLastAudioSource();
                launchBrightnessControllerService();

                registerOnAllAttachedListener(new AllDevicesAttachedListener() {
                    @Override
                    public void onAllAttached() {
                        Log.v(TAG, "All devices attached");
                        launchGpsService();
                        startInputService();
                    }

                    @Override
                    public void onTimedOut() {
                        Log.v(TAG, "Timed out waiting for devices to connect");
                    }
                });

                if (!sharedPreferences.getBoolean(getString(R.string.DEBUG_ENABLED_KEY), true)) {
                    raiseVolume();
                }

                if (sharedPreferences.getBoolean(getString(R.string.LAUNCH_MAPS_KEY), false) &&
                        sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_KEY), false)) {
                    Log.v(TAG, "Launching maps");
                    launchMaps();
                }
            } else {
                Log.v(TAG, "Aborting start routine as it has " +
                        "already been run once while power is connected");
            }
        } else {
            Log.v(TAG, "Power no longer connected, aborting");
        }
    }

    public void runStopSequence() {
        Log.v(TAG, "Running stop routine");
        if (lastState == START_ROUTINE_RUN) {
            lastState = STOP_ROUTINE_RUN;
            stopBlankAudio();
            checkMapsForeground();

        } else {
            Log.v(TAG, "Aborting, stop routine already run");
        }

    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.v(TAG, intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_POWER_CONNECTED:
                startBlankAudio();
                acquireWakeLock();

                int delay = 1000; //milliseconds
                new Handler(getMainLooper()).postDelayed(new Runnable() {
                    public void run() {
                        runStartRoutine();
                    }
                }, delay);

                return START_NOT_STICKY;

            case Intent.ACTION_POWER_DISCONNECTED:
                stopBlankAudio();
                releaseWakeLock();
                runStopSequence();

                return START_NOT_STICKY;
        }
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

    public String getForegroundPackageName(){
        String currentApp = "";

        //noinspection ResourceType
        UsageStatsManager usm = (UsageStatsManager) getSystemService(CONTEXT_USAGE_STATS_MANAGER);

        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 1000, time);
        // Gets the apps used in the last second

        Log.d(TAG, String.valueOf(appList.size()));

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

}
