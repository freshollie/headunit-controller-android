package com.freshollie.headunitcontroller.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.freshollie.headunitcontroller.R;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class PowerUtil {
    private static Boolean debug = false;
    private static Boolean debugPowerOn = false;

    public static boolean isConnected(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.getBoolean(context.getString(R.string.pref_debug_enabled_key), false)) {

            return sharedPreferences
                    .getBoolean(context.getString(R.string.pref_power_on_debug_key), false);

        } else {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB;
        }
    }
}
