package com.freshollie.headunitcontroller.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.utils.NotificationHandler;
import com.freshollie.headunitcontroller.utils.PowerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Freshollie on 13/12/2016.
 */

public class MediaMonitor implements MediaSessionManager.OnActiveSessionsChangedListener{

    public String TAG = this.getClass().getSimpleName();
    private MediaSessionManager mediaSessionManager;

    private String mainPlaybackController = "";

    // Used to keep track of the currently playing media controllers
    // With the aim of the controller at the front being the current controller
    private ArrayList<String> playingControllersQueue = new ArrayList<>();

    private NotificationHandler notificationHandler;

    private SharedPreferences sharedPreferences;

    private Handler mainHandler;
    private Runnable setLastPlaybackNullRunnable = new Runnable() {
        @Override
        public void run() {
            recordActivePlaybackApp(null);
        }
    };

    private Context context;

    public MediaMonitor(Context serviceContext) {
        Log.v(TAG, "Initialised");

        context = serviceContext;
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(
                        context.getString(R.string.PREFERENCES_KEY),
                        Context.MODE_PRIVATE
                );

        mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mainHandler = new Handler(context.getMainLooper());
    }

    public void start() {
        bindActiveSessionsChangedListener();
    }

    public void stop() {
        mediaSessionManager.removeOnActiveSessionsChangedListener(this);
    }

    public void bindActiveSessionsChangedListener() {
        mediaSessionManager.addOnActiveSessionsChangedListener(
                this,
                new ComponentName(context, MapsListenerService.class)
        );
    }

    /**
     * Called if any new app starts or controlling music.
     * If will store the list of MediaControllers for later
     *
     * @param list of MediaControllers
     */
    @Override
    public void onActiveSessionsChanged(List<MediaController> list) {
        Log.i(TAG, list.size() + " active media controllers");
        checkMediaControllerList(list);
    }

    public void checkMediaControllerList(List<MediaController> mediaControllers) {
        for (MediaController mediaController: mediaControllers) {
            Log.v(TAG, mediaController.getPackageName());
            if (!(mediaController.getPackageName().equals(context.getPackageName()))) {
                PlaybackState playbackState = mediaController.getPlaybackState();
                if (playbackState != null) {
                    if (playbackState.getState() == PlaybackState.STATE_PLAYING) {
                        recordActivePlaybackApp(mediaController);
                    }
                } else if (mediaController.getPackageName().equals("com.freshollie.radioapp")){
                    recordActivePlaybackApp(mediaController);
                }
            }
        }
    }

    /**
     * We know that the active media controllers have changed, so
     * check which ones in the list are still active.
     *
     * Then after a delay, record the first to a list
     */
    public void onActiveControllerPlaybackStopped(String controllerPackage) {
        Log.v(TAG, "Playback stopped on: " + controllerPackage);
        playingControllersQueue.remove(controllerPackage);
        if (playingControllersQueue.size() < 1) {
            saveLastPlaybackController("");
        } else {
            saveLastPlaybackController(playingControllersQueue.get(0));
        }
    }

    public void saveLastPlaybackController(String packageName) {
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

                Log.v(TAG, "Recording last playback app " + String.valueOf(outputPackageName));

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(context.getString(R.string.PLAYING_AUDIO_APP_KEY), packageName);
                editor.apply();

                mainPlaybackController = packageName;
            }
        }
    }

    public void recordActivePlaybackApp(final MediaController playbackMediaController) {
        if (PowerUtil.isConnected(context.getApplicationContext())) {

            if (!playingControllersQueue.contains(playbackMediaController.getPackageName())) {
                playingControllersQueue.add(playbackMediaController.getPackageName());
            }
            saveLastPlaybackController(playbackMediaController.getPackageName());

            playbackMediaController.registerCallback(new MediaController.Callback() {

                Runnable removeRunnable = new Runnable() {
                    @Override
                    public void run() {
                        onActiveControllerPlaybackStopped(playbackMediaController.getPackageName());
                    }
                };

                /**
                 * Register a callback to wait for the music app to stop playing music.
                 * If it stops playing then we make sure the music app wont be played on run
                 *
                 * @param state Playback state of the app
                 */
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    super.onPlaybackStateChanged(state);

                    if (state.getState() == PlaybackState.STATE_PLAYING) {
                        mainHandler.removeCallbacks(removeRunnable);
                        saveLastPlaybackController(playbackMediaController.getPackageName());

                    } else {
                        // Because the state is not playing we assume not active
                        // So remove the callback
                        playbackMediaController.unregisterCallback(this);

                        // It will be re-added if the active controllers change again

                        mainHandler.postDelayed(removeRunnable, 2000);
                    }
                }
            });
        }
    }

}
