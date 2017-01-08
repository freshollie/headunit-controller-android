package com.freshollie.headunitcontroller.input;

import android.content.Context;
import android.content.SharedPreferences;

import com.freshollie.headunitcontroller.R;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

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

    public void setButtonAction(int id, String action, String extra, boolean hold, long holdLength) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!hold) {
            editor.putString(context.getString(R.string.BUTTON_PRESS_EVENT_KEY, id),
                    action);

            editor.putString(context.getString(R.string.BUTTON_PRESS_EXTRA_DATA_KEY, id),
                    extra);


        } else {
            editor.putString(context.getString(R.string.BUTTON_HOLD_EVENT_KEY, id),
                    action);

            editor.putString(context.getString(R.string.BUTTON_HOLD_EXTRA_DATA_KEY, id),
                    extra);

            editor.putLong(
                    context.getString(R.string.BUTTON_HOLD_LENGTH_KEY, id),
                    holdLength);
        }
        editor.apply();
    }

    public void setButtonAction(int id, String action, String extra) {
        setButtonAction(id, action, extra, false, 0);
    }

    public void setButtonAction(int id, String action, boolean hold, long holdLength) {
        setButtonAction(id, action, null, hold, holdLength);
    }

    public void setButtonAction(int id, String action) {
        setButtonAction(id, action, null, false, 0);
    }

    public void setRingAction(int position, String action, String extra, boolean hold, long holdLength) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!hold) {
            editor.putString(context.getString(R.string.RING_PRESS_EVENT_KEY, position),
                    action);

            editor.putString(context.getString(R.string.RING_PRESS_EXTRA_DATA_KEY, position),
                    extra);
        } else {
            editor.putString(context.getString(R.string.RING_HOLD_EVENT_KEY, position),
                    action);

            editor.putString(context.getString(R.string.RING_HOLD_EXTRA_DATA_KEY, position),
                    extra);

            editor.putLong(
                    context.getString(R.string.RING_HOLD_LENGTH_KEY, position),
                    holdLength);
        }

        editor.apply();
    }

    public void setRingAction(int position, String action, String extra) {
        setButtonAction(position, action, extra, false, 0);
    }

    public void setRingAction(int position, String action, boolean hold, int holdLength) {
        setButtonAction(position, action, null, hold, holdLength);
    }

    public void setRingAction(int position, String action) {
        setButtonAction(position, action, null, false, 0);
    }

    public void setWheelAction(int wheelAction, String action, String extra) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (wheelAction) {
            case ShuttleXpressDevice.ACTION_LEFT:
                editor.putString(context.getString(R.string.WHEEL_LEFT_EVENT_KEY),
                        action);
                editor.putString(context.getString(R.string.WHEEL_LEFT_EXTRA_DATA_KEY),
                        extra);

            case ShuttleXpressDevice.ACTION_RIGHT:
                editor.putString(context.getString(R.string.WHEEL_RIGHT_EVENT_KEY),
                        action);

                editor.putString(context.getString(R.string.WHEEL_RIGHT_EXTRA_DATA_KEY),
                        extra);

        }
    }

    public void setWheelAction(int wheelAction, String action) {
        setWheelAction(wheelAction, action, null);
    }

    public String[] getButtonPressAction(int id) {
        return new String[]{
                sharedPreferences.getString(
                        context.getString(R.string.BUTTON_PRESS_EVENT_KEY, id),
                        null
                ),

                sharedPreferences.getString(
                        context.getString(R.string.BUTTON_PRESS_EXTRA_DATA_KEY, id),
                        null
                )
        };
    }

    public String[] getButtonHoldAction(int id) {
        return new String[]{
                sharedPreferences.getString(
                        context.getString(R.string.BUTTON_HOLD_EVENT_KEY, id),
                        null
                ),

                sharedPreferences.getString(
                        context.getString(R.string.BUTTON_HOLD_EXTRA_DATA_KEY, id),
                        null
                )
        };
    }

    public String[] getRingPressAction(int position) {
        return new String[]{
                sharedPreferences.getString(
                        context.getString(R.string.RING_PRESS_EVENT_KEY, position),
                        null
                ),

                sharedPreferences.getString(
                        context.getString(R.string.RING_PRESS_EXTRA_DATA_KEY, position),
                        null
                )
        };
    }

    public String[] getRingHoldAction(int position) {
        return new String[]{
                sharedPreferences.getString(
                        context.getString(R.string.RING_HOLD_EVENT_KEY, position),
                        null
                ),

                sharedPreferences.getString(
                        context.getString(R.string.RING_HOLD_EXTRA_DATA_KEY, position),
                        null
                )
        };
    }

    public String[] getWheelAction(int wheelAction) {
        if (wheelAction == ShuttleXpressDevice.ACTION_LEFT) {
            return new String[]{
                    sharedPreferences.getString(
                            context.getString(R.string.WHEEL_LEFT_EVENT_KEY),
                            null
                    ),

                    sharedPreferences.getString(
                            context.getString(R.string.WHEEL_LEFT_EXTRA_DATA_KEY),
                            null
                    )
            };
        } else {
            return new String[]{
                    sharedPreferences.getString(
                            context.getString(R.string.WHEEL_RIGHT_EVENT_KEY),
                            null
                    ),

                    sharedPreferences.getString(
                            context.getString(R.string.WHEEL_RIGHT_EXTRA_DATA_KEY),
                            null
                    )
            };
        }
    }

    public long getButtonHoldDelay(int id) {
        return sharedPreferences.getLong(
                context.getString(R.string.BUTTON_HOLD_LENGTH_KEY, id),
                0);
    }

    public long getRingHoldDelay(int position) {
        return sharedPreferences.getLong(
                context.getString(R.string.RING_HOLD_LENGTH_KEY, position),
                0);
    }
}
