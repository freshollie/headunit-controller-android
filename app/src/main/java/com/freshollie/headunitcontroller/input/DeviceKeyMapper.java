package com.freshollie.headunitcontroller.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
