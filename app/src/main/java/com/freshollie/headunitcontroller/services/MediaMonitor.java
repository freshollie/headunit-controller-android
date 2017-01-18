package com.freshollie.headunitcontroller.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.utils.NotificationHandler;
import com.freshollie.headunitcontroller.utils.PowerUtil;

import java.util.List;

/**
 * Created by Freshollie on 13/12/2016.
 */

public class MediaMonitor implements MediaSessionManager.OnActiveSessionsChangedListener{

    public String TAG = this.getClass().getSimpleName();
    private MediaController lastMusicPlaybackController;
    private MediaSessionManager mediaSessionManager;

    private NotificationHandler notificationHandler;

    private SharedPreferences sharedPreferences;

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
                        recordLastPlaybackApp(mediaController);
                    }
                } else if (mediaController.getPackageName().equals("com.freshollie.radioapp")){
                    recordLastPlaybackApp(mediaController);
                }
            }
        }
    }

    public void recordLastPlaybackApp(final MediaController appMediaController) {
        if (PowerUtil.isConnected(context.getApplicationContext())) {
            lastMusicPlaybackController = appMediaController;
            String packageName = "";

            if (appMediaController != null) {
                appMediaController.registerCallback(new MediaController.Callback() {
                    /**
                     * Register a callback to wait for the music app to stop playing music.
                     * If it stops playing then we make sure the music app wont be played on run
                     *
                     * @param state Playback state of the app
                     */
                    @Override
                    public void onPlaybackStateChanged(PlaybackState state) {
                        super.onPlaybackStateChanged(state);
                        appMediaController.unregisterCallback(this);

                        if (state.getState() == PlaybackState.STATE_PLAYING) {
                            recordLastPlaybackApp(appMediaController);
                        } else {
                            // Check if the current last playback app is this app
                            if (lastMusicPlaybackController == appMediaController) {
                                recordLastPlaybackApp(null);
                            }
                        }
                    }
                });
                packageName = appMediaController.getPackageName();
            }

            if (!packageName.equals(
                    sharedPreferences.getString(
                            context.getString(R.string.PLAYING_AUDIO_APP_KEY),
                            ""))
                    ) {

                String outputPackageName = packageName;

                if (outputPackageName.isEmpty()){
                    outputPackageName = null;
                }

                Log.v(TAG, "Recording last playback app " + String.valueOf(outputPackageName));

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(context.getString(R.string.PLAYING_AUDIO_APP_KEY), packageName);
                editor.apply();
            }
        }
    }

}
