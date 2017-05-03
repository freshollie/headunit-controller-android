package com.freshollie.headunitcontroller.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

    public static class ActionMap {
        private int actionId;
        private String extra;

        public ActionMap(int actionId, String extra) {
            this.actionId = actionId;
            this.extra = extra;
        }

        public void setActionId(int actionId) {
            this.actionId = actionId;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }

        public String getAction() {
            return DeviceInputManager.getActionFromId(actionId);
        }
        public int getActionId() {
            return actionId;
        }

        public String getExtra() {
            return extra;
        }

        public String getReadableExtra(Context context) {
            if (getAction().equals(DeviceInputManager.ACTION_SEND_KEYEVENT)) {
                int keyCode = Integer.parseInt(getExtra());
                return KeyEvent.keyCodeToString(keyCode);

            } else if (getAction().equals(DeviceInputManager.ACTION_LAUNCH_APP)) {
                String packageName = getExtra();
                PackageManager packageManager= context.getPackageManager();
                String appName = "";
                if (!packageName.isEmpty()) {
                    try {
                        appName = (String) packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(packageName,
                                        PackageManager.GET_META_DATA)
                        );
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                    }
                }
                return appName;

            } else {
                return null;
            }
        }
    }

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
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_LAUNCH_APP),
                "com.spotify.music",
                true,
                300);
        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_0,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_SEND_KEYEVENT),
                String.valueOf(KeyEvent.KEYCODE_ENTER)
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_1,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_LAUNCH_APP),
                "com.freshollie.monkeyboarddabradio"
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_2,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_LAUNCH_APP),
                "au.com.shiftyjelly.pocketcasts"
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_LAUNCH_APP),
                "com.google.android.apps.maps"
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_3,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_START_DRIVING_MODE),
                true,
                1000
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_GO_HOME)
        );
        setKeyAction(
                ShuttleXpressDevice.KeyCodes.BUTTON_4,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_LAUNCH_VOICE_ASSIST),
                true,
                1000
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_SEND_KEYEVENT),
                String.valueOf(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        );
        setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_LEFT,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_SEND_KEYEVENT),
                String.valueOf(KeyEvent.KEYCODE_BACK),
                true,
                1000
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.RING_RIGHT,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_SEND_KEYEVENT),
                String.valueOf(KeyEvent.KEYCODE_MEDIA_NEXT)
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_LEFT,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_SEND_KEYEVENT),
                String.valueOf(KeyEvent.KEYCODE_DPAD_UP)
        );

        setKeyAction(
                ShuttleXpressDevice.KeyCodes.WHEEL_RIGHT,
                DeviceInputManager.getIdFromAction(DeviceInputManager.ACTION_SEND_KEYEVENT),
                String.valueOf(KeyEvent.KEYCODE_TAB)
        );
    }

    public void setKeyAction(int id, int actionId, String extra, boolean hold, int holdLength) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!hold) {
            editor.putInt(context.getString(R.string.pref_key_press_action_key, id),
                    actionId);

            editor.putString(context.getString(R.string.pref_key_press_extra_data_key, id),
                    extra);


        } else {
            editor.putInt(context.getString(R.string.pref_key_hold_action_key, id),
                    actionId);

            editor.putString(context.getString(R.string.pref_key_hold_extra_data_key, id),
                    extra);

            editor.putInt(
                    context.getString(R.string.pref_key_hold_length_key, id),
                    holdLength);
        }
        editor.apply();
    }

    public void setKeyAction(int id, int actionId, String extra) {
        setKeyAction(id, actionId, extra, false, 0);
    }

    public void setKeyAction(int id, int actionId, boolean hold, int holdLength) {
        setKeyAction(id, actionId, null, hold, holdLength);
    }

    public void setKeyAction(int id, int actionId) {
        setKeyAction(id, actionId, null, false, 0);
    }
    
    public ActionMap getKeyPressAction(int id) {
        return new ActionMap(
                sharedPreferences.getInt(
                        context.getString(R.string.pref_key_press_action_key, id),
                        0
                ),

                sharedPreferences.getString(
                        context.getString(R.string.pref_key_press_extra_data_key, id),
                        null
                )
        );
    }

    public ActionMap getKeyHoldAction(int id) {
        return new ActionMap(
                sharedPreferences.getInt(
                        context.getString(R.string.pref_key_hold_action_key, id),
                        0
                ),

                sharedPreferences.getString(
                        context.getString(R.string.pref_key_hold_extra_data_key, id),
                        null
                )
        );
    }

    public int getKeyHoldDelay(int id) {
        return sharedPreferences.getInt(context.getString(R.string.pref_key_hold_length_key, id), 0);
    }

    public void clear(int key) {
        setKeyAction(key, -1, null);
        setKeyAction(key, -1, null, true, 0);
    }

    public void clearAll() {
        for (int i = 0; i < ShuttleXpressDevice.KeyCodes.NUM_KEYS; i++) {
            clear(ShuttleXpressDevice.KeyCodes.BUTTON_0 + i);
        }
    }
}
