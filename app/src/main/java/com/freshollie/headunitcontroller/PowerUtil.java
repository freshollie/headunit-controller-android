package com.freshollie.headunitcontroller;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class PowerUtil {
    private static Boolean debug = false;
    private static Boolean debugPowerOn = false;

    public static boolean isConnected(Context context) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);

        if (sharedPreferences.getBoolean(context.getString(R.string.DEBUG_ENABLED_KEY), false)) {

            return sharedPreferences
                    .getBoolean(context.getString(R.string.POWER_ON_DEBUG_KEY), false);

        } else {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB;
        }
    }
}
