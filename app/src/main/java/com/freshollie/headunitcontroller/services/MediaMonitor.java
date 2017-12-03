package com.freshollie.headunitcontroller.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.util.PowerUtil;
import com.freshollie.headunitcontroller.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Freshollie on 13/12/2016.
 */

public class MediaMonitor extends MediaController.Callback implements
        MediaSessionManager.OnActiveSessionsChangedListener{
    private static final String TAG = MediaMonitor.class.getSimpleName();

    private MediaSessionManager mediaSessionManager;

    // Used to keep track of the currently playing media controllers
    private ArrayList<MediaController> activeMediaControllers = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    private Handler mainHandler;

    private Context context;

    MediaMonitor(Context serviceContext) {
        Log.d(TAG, "Created");

        context = serviceContext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mainHandler = new Handler(context.getMainLooper());
    }

    void start() {
        Logger.log(TAG, "Starting Media Monitor");

        mediaSessionManager.addOnActiveSessionsChangedListener(
                this,
                new ComponentName(context, GoogleMapsListenerService.class)
        );
    }

    void stop() {
        Logger.log(TAG, "Stopping Media Monitor");

        mediaSessionManager.removeOnActiveSessionsChangedListener(this);
    }

    /**
     * Called if any new app starts controlling music.
     * If will store the list of MediaControllers for later
     *
     * @param newMediaControllersList list of active media controllers
     */
    @Override
    public void onActiveSessionsChanged(List<MediaController> newMediaControllersList) {
        Log.d(TAG, newMediaControllersList.size() + " active media controllers: ");

        // Clear the old media controllers and callbacks
        for (MediaController oldMediaController : activeMediaControllers) {
            oldMediaController.unregisterCallback(this);
        }
        activeMediaControllers.clear();

        // Track the media controllers
        for (MediaController newMediaController: newMediaControllersList) {
            // Make sure we don't track our own playback
            Log.d(TAG, newMediaController.getPackageName());
            if (!newMediaController.getPackageName().equals(context.getPackageName())) {
                activeMediaControllers.add(newMediaController);
                newMediaController.registerCallback(this);
            }
        }

        // Check the new media controllers
        checkActiveMediaControllers();
    }

    private Runnable runnableSetNoPlayback = new Runnable() {
        @Override
        public void run() {
            saveCurrentActiveMediaApp("");
        }
    };

    private void checkActiveMediaControllers() {
        boolean mediaControllerActive = false;

        for (MediaController activeMediaController: activeMediaControllers) {
            PlaybackState playbackState = activeMediaController.getPlaybackState();
            if (playbackState != null) {
                if (playbackState.getState() == PlaybackState.STATE_PLAYING) {
                    mediaControllerActive = true;
                    mainHandler.removeCallbacks(runnableSetNoPlayback);
                    saveCurrentActiveMediaApp(activeMediaController.getPackageName());
                    break;
                }
            }
        }

        if (!mediaControllerActive) {
            mainHandler.postDelayed(runnableSetNoPlayback, 2000);
        }
    }


    private void saveCurrentActiveMediaApp(String packageName) {
        if (PowerUtil.isConnected(context.getApplicationContext())) {
            if (!packageName.equals(
                    sharedPreferences.getString(
                            context.getString(R.string.PLAYING_AUDIO_APP_KEY),
                            ""))
                    ) {

                String outputPackageName = packageName;

                if (outputPackageName.isEmpty()) {
                    outputPackageName = null;
                }

                Logger.log(TAG, "Last playback app: " + String.valueOf(outputPackageName));

                sharedPreferences.edit()
                        .putString(context.getString(R.string.PLAYING_AUDIO_APP_KEY), packageName)
                        .apply();
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        super.onPlaybackStateChanged(state);
        checkActiveMediaControllers();
    }
}
