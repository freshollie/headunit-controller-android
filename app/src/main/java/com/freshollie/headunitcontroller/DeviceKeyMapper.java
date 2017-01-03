package com.freshollie.headunitcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

/**
 * Created by freshollie on 1/3/17.
 */

public class DeviceKeyMapper {
    SharedPreferences sharedPreferences;
    Context context;

    public DeviceKeyMapper(Context appContext) {
        sharedPreferences = appContext.getSharedPreferences(
                appContext.getString(R.string.PREFERENCES_KEY),
                Context.MODE_PRIVATE
        );

        context = appContext;
    }

    public void setButton(int id, String action, String extra, boolean hold) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!hold) {
            editor.putString(context.getString(R.string.BUTTON_PRESS_EVENT_KEY, 0),
                    action);

            switch (action) {
                case DeviceInputService.ACTION_LAUNCH_APP:

            }
        }

        editor.putString(context.getString(R.string.BUTTON_HOLD_EVENT_KEY, 0),
                DeviceInputService.ACTION_LAUNCH_APP);
        editor.putString(context.getString(R.string.BUTTON_HOLD_LAUNCH_APP_PACKAGE_KEY, 0),
                "com.apple.android.music");
    }
}
