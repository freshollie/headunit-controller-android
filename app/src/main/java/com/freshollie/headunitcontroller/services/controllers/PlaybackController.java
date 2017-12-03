package com.freshollie.headunitcontroller.services.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.util.Logger;
import com.freshollie.headunitcontroller.util.SuperuserManager;

/**
 * Created by freshollie on 03.12.17.
 */

public class PlaybackController {
    private static final String TAG = PlaybackController.class.getSimpleName();

    private static final String APPLE_MUSIC_PACKAGE_ID = "com.apple.android.music";
    private static final String APPLE_MUSIC_PLAY_ACTION_COMMAND =
            "am startservice " +
                    "-a 'com.apple.music.client.player.play_pause' " +
                    "-n com.apple.android.music/com.apple.android.svmediaplayer.player.MusicService";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final AudioManager audioManager;
    private final SuperuserManager superuserManager;

    private MediaPlayer mediaPlayer;

    public PlaybackController(Context serviceContext) {
        context = serviceContext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        superuserManager = SuperuserManager.getInstance();

        mediaPlayer = MediaPlayer.create(context, R.raw.blank);
        mediaPlayer.setLooping(true);
    }

    private void startBlankAudio() {
        stopBlankAudio();
        mediaPlayer.start();
        Log.d(TAG, "Blank audio started");
    }

    private void stopBlankAudio(){
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "Blank audio stopped");
        }
    }

    private void raiseVolume() {
        int volume = Integer.valueOf(
                sharedPreferences.getString(
                        context.getString(R.string.pref_volume_level_key),
                        "13"
                )
        );

        Log.d(TAG, "Raising system volume to " + volume);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }


    /**
     * Run generic play command on given audio app.
     *
     * If the app has a specific method to play, then that method will be run
     * @param packageName
     */
    private void playAudioApp(String packageName) {
        switch (packageName) {
            case APPLE_MUSIC_PACKAGE_ID:
                if (superuserManager.hasPermission()) {
                    superuserManager.asyncExecute(APPLE_MUSIC_PLAY_ACTION_COMMAND);
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

    private void playLastAudioSource() {
        String lastPlayingAudioApp =
                sharedPreferences.getString(context.getString(R.string.PLAYING_AUDIO_APP_KEY), "");

        if (!lastPlayingAudioApp.isEmpty()) {
            Logger.log(TAG, "StartUp: Playing " + lastPlayingAudioApp);
            playAudioApp(lastPlayingAudioApp);
        }
    }

    public void onStartup() {
        if (!sharedPreferences.getBoolean(
                context.getString(R.string.pref_debug_enabled_key),
                false) &&
                sharedPreferences.getBoolean(
                        context.getString(R.string.pref_set_volume_key),
                        true)) {

            Logger.log(TAG, "StartUp: Setting Volume");
            raiseVolume();
        }

        if (sharedPreferences.getBoolean(
                context.getString(R.string.pref_play_media_key), true)) {
            Logger.log(TAG, "StartUp: playing last media");
            playLastAudioSource();
        }
    }

    public void onSuspend() {
        // Here we would stop music, but this is handled by timurs kernel
    }

    public void onPowerConnected() {
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_blank_audio_key), true)) {
            Logger.log(TAG, "Starting blank audio");
            startBlankAudio();
        }
    }

    public void onPowerDisconnected() {
        if (mediaPlayer.isPlaying()) {
            Logger.log(TAG, "Stopping blank audio");
            stopBlankAudio();
        }
    }

    public void destroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}
