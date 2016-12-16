package com.freshollie.headunitcontroller;

import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class RoutineService extends Service {
    public String TAG = "RoutineService";
    public static int ALL_DEVICES = 0;
    public static int ATTACH_TIMEOUT = 3000; // Milliseconds

    public static String CONTEXT_USAGE_STATS_MANAGER = "usagestats";

    private SharedPreferences sharedPreferences;
    private MediaPlayer mediaPlayer;

    public interface OnAllDevicesAttachedListener{
        void onAllAttached();
    }

    @Override
    public void onCreate() {
        sharedPreferences = getSharedPreferences(
                getString(R.string.PREFERENCES_KEY),
                Context.MODE_PRIVATE
        );

    }

    public void playBlankAudio() {
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
        ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
                .setStreamVolume(AudioManager.STREAM_MUSIC, 13, 0);
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
                startService(
                        new Intent("com.apple.music.client.player.play_pause").setComponent(
                                new ComponentName(
                                    "com.apple.android.music",
                                    "com.apple.android.svmediaplayer.player.MusicService"
                                )
                        )
                );
                return;

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

    public void launchMaps() {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"))
                .setPackage("come.google.android.apps.maps");
        startActivity(intent);
    }

    public void checkMapsForeground() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (getForegroundPackageName().equals("com.google.android.apps.maps")) {
            editor.putBoolean(getString(R.string.LAUNCH_MAPS_KEY), true);
        } else {
            editor.putBoolean(getString(R.string.LAUNCH_MAPS_KEY), false);
        }
        editor.apply();
    }

    public void runStartRoutine() {
        Log.v(TAG, "Running start routine");
        playLastAudioSource();
        //raiseVolume();

        if (sharedPreferences.getBoolean(getString(R.string.LAUNCH_MAPS_KEY), false) &&
                sharedPreferences.getBoolean(getString(R.string.DRIVING_MODE_KEY), false)) {
            launchMaps();
        }
    }

    public void runStopSequence() {
        Log.v(TAG, "Running stop routine");
        stopBlankAudio();
        checkMapsForeground();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.v(TAG, intent.getAction());

        switch (intent.getAction()) {
            case Intent.ACTION_POWER_CONNECTED:
                Handler handler = new Handler(getMainLooper());
                int delay = 1000; //milliseconds

                handler.postDelayed(new Runnable() {
                    public void run() {
                        runStartRoutine();
                    }
                }, delay);
                return START_NOT_STICKY;

            case Intent.ACTION_POWER_DISCONNECTED:
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
