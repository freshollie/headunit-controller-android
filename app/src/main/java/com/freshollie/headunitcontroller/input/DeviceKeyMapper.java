package com.freshollie.headunitcontroller.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import com.freshollie.headunitcontroller.R;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

/**
 * Created by freshollie on 1/3/17.
 */

public class DeviceKeyMapper {
    private SharedPreferences sharedPreferences;
    private Context context;

    public DeviceKeyMapper(Context appContext) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        context = appContext;

        if (sharedPreferences.getBoolean("firstrun", true)) {
            sharedPreferences.edit().putBoolean("firstrun", false).apply();
            setDefaults();
        }

    }

    /**
     * Set the test bindings for the input device
     */
    public void setDefaults() {
        clearAll();

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "com.spotify.music",
                true,
                300);
        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_ENTER)
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_1,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "com.freshollie.monkeyboarddabradio"
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_2,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "au.com.shiftyjelly.pocketcasts"
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputManager.ACTION_LAUNCH_APP,
                "com.google.android.apps.maps"
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputManager.ACTION_START_DRIVING_MODE,
                true,
                1000
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputManager.ACTION_GO_HOME
        );
        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputManager.ACTION_LAUNCH_VOICE_ASSIST,
                true,
                1000
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        );
        setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_BACK),
                true,
                1000
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_RIGHT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_NEXT)
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_LEFT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_DPAD_UP)
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_RIGHT,
                DeviceInputManager.ACTION_SEND_KEYEVENT,
                String.valueOf(KeyEvent.KEYCODE_TAB)
        );
    }

    public void setKeyAction(int id, String action, String extra, boolean hold, long holdLength) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!hold) {
            editor.putString(context.getString(R.string.pref_key_press_event_key, id),
                    action);

            editor.putString(context.getString(R.string.pref_key_press_extra_data_key, id),
                    extra);


        } else {
            editor.putString(context.getString(R.string.pref_key_hold_event_keu, id),
                    action);

            editor.putString(context.getString(R.string.pref_key_hold_extra_data_key, id),
                    extra);

            editor.putLong(
                    context.getString(R.string.pref_key_hold_length_key, id),
                    holdLength);
        }
        editor.apply();
    }

    public void setKeyAction(int id, String action, String extra) {
        setKeyAction(id, action, extra, false, 0);
    }

    public void setKeyAction(int id, String action, boolean hold, long holdLength) {
        setKeyAction(id, action, null, hold, holdLength);
    }

    public void setKeyAction(int id, String action) {
        setKeyAction(id, action, null, false, 0);
    }
    
    public String[] getKeyPressAction(int id) {
        return new String[]{
                sharedPreferences.getString(
                        context.getString(R.string.pref_key_press_event_key, id),
                        null
                ),

                sharedPreferences.getString(
                        context.getString(R.string.pref_key_press_extra_data_key, id),
                        null
                )
        };
    }

    public String[] getKeyHoldAction(int id) {
        return new String[]{
                sharedPreferences.getString(
                        context.getString(R.string.pref_key_hold_event_keu, id),
                        null
                ),

                sharedPreferences.getString(
                        context.getString(R.string.pref_key_hold_extra_data_key, id),
                        null
                )
        };
    }

    public long getKeyHoldDelay(int id) {
        return sharedPreferences.getLong(context.getString(R.string.pref_key_hold_length_key, id), 0);
    }

    public void clear(int key) {
        setKeyAction(key, null, null);
        setKeyAction(key, null, null, true, 0);
    }

    public void clearAll() {
        for (int i = 0; i < ShuttleXpressDevice.KeyCodes.NUM_KEYS; i++) {
            clear(ShuttleXpressDevice.KeyCodes.BUTTON_0 + i);
        }
    }
}
