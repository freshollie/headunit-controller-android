package com.freshollie.headunitcontroller.ui.settings.fragments;

/**
 * Created by freshollie on 03.12.17.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.services.input.DeviceInputManager;
import com.freshollie.headunitcontroller.services.input.DeviceKeyMapper;
import com.freshollie.headunitcontroller.services.MainService;
import com.freshollie.headunitcontroller.ui.settings.DummyPreference;
import com.freshollie.headunitcontroller.ui.settings.KeySetDialog;
import com.freshollie.headunitcontroller.SettingsActivity;
import com.freshollie.shuttlexpress.ShuttleXpressDevice;

/**
 * This fragment shows the external input preferences, it is generated from the Shuttle Xpress
 * device API,
 *
 */
public class InputPreferencesFragment extends PreferenceFragment {
    DeviceKeyMapper keyMapper;

    Preference[] deviceKeyPreferences = new Preference[ShuttleXpressDevice.KeyCodes.NUM_KEYS];

    Preference defaultsPreference;
    Preference startInputPreference;

    PreferenceCategory buttonCategory;
    PreferenceCategory wheelCategory;
    PreferenceCategory ringCategory;
    SwitchPreference inputEnabledPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        keyMapper = new DeviceKeyMapper(getActivity());

        createScreen();
        updateScreen();
    }

    private String getSummaryForKey(int key, boolean hold) {
        String summary = "";

        DeviceKeyMapper.ActionMap actionAndExtra;

        if (!hold) {
            actionAndExtra = keyMapper.getKeyPressAction(key);
        } else {
            actionAndExtra = keyMapper.getKeyHoldAction(key);
        }

        if (actionAndExtra.getActionId() != -1) {
            if (hold) {
                summary += "Hold " + keyMapper.getKeyHoldDelay(key) + "ms: " +
                        DeviceInputManager.getStringForAction(getActivity(),
                                actionAndExtra.getActionId());
            } else {
                summary += "Press: " +
                        DeviceInputManager.getStringForAction(getActivity(),
                                actionAndExtra.getActionId());
            }

            String readableExtra = actionAndExtra.getReadableExtra(getActivity());
            if (readableExtra != null) {
                summary += " -> " + readableExtra;
            }

        } else {
            if (hold) {
                summary += "Hold: None";
            } else {
                summary += "Press: None";
            }
        }

        if (!hold) {
            summary += "\n";
            summary += getSummaryForKey(key, true);
        }

        return summary;
    }

    public String getSummaryForKey(int key) {
        return getSummaryForKey(key, false);
    }

    private void launchKeySetDialog(int key) {
        KeySetDialog dialog = new KeySetDialog();

        // Page will refresh when dialog closes
        dialog.setOnDismissListener(new KeySetDialog.KeySetDismissListener() {
            @Override
            public void onDismissed() {
                updateScreen();
            }
        });

        dialog.setKey(key);
        dialog.show(getFragmentManager(), KeySetDialog.class.getSimpleName());
    }

    private void updateScreen() {
        boolean inputEnabled = inputEnabledPreference.isChecked();

        defaultsPreference.setEnabled(inputEnabled);
        buttonCategory.setEnabled(inputEnabled);
        ringCategory.setEnabled(inputEnabled);
        wheelCategory.setEnabled(inputEnabled);
        startInputPreference.setEnabled(inputEnabled);

        for (int i = 0; i < deviceKeyPreferences.length; i++) {
            int key = ShuttleXpressDevice.KeyCodes.ALL_KEYS[i];
            deviceKeyPreferences[i].setSummary(getSummaryForKey(key));
            deviceKeyPreferences[i].setEnabled(inputEnabled);
        }
    }

    private void createScreen() {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());

        DummyPreference d = new DummyPreference(getActivity(), null);
        screen.addPreference(d);

        inputEnabledPreference = new SwitchPreference(getActivity());
        inputEnabledPreference.setTitle(R.string.pref_input_service_enabled_title);
        inputEnabledPreference.setDefaultValue(true);
        inputEnabledPreference.setKey(getString(R.string.pref_input_service_enabled_key));
        inputEnabledPreference.setSummaryOn(R.string.pref_input_service_enabled_summary_on);
        inputEnabledPreference.setSummaryOff(R.string.pref_input_service_enabled_summary_off);
        inputEnabledPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean value = (boolean) o;

                for (Preference keyPreference: deviceKeyPreferences) {
                    keyPreference.setEnabled(value);
                }

                startInputPreference.setEnabled(value);
                defaultsPreference.setEnabled(value);

                buttonCategory.setEnabled(value);
                wheelCategory.setEnabled(value);
                ringCategory.setEnabled(value);
                return true;
            }
        });
        screen.addPreference(inputEnabledPreference);

        startInputPreference = new Preference(getActivity());
        startInputPreference.setTitle(R.string.pref_launch_input_title);
        startInputPreference.setSummary(R.string.pref_launch_input_summary);
        startInputPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MainService.start(getActivity(), MainService.ACTION_START_INPUT_SERVICE);
                return true;
            }
        });
        screen.addPreference(startInputPreference);

        defaultsPreference = new Preference(getActivity());
        defaultsPreference.setTitle("Reset to default");
        defaultsPreference.setSummary("Reset all input settings to their original values");
        defaultsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(
                                "Are you sure you want to set the input preferences to default?")
                        .setPositiveButton(android.R.string.no, null)
                        .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                keyMapper.setDefaults();
                                updateScreen();
                            }
                        })
                        .show();
                return true;
            }
        });
        screen.addPreference(defaultsPreference);

        for (int i = 0; i < deviceKeyPreferences.length; i++) {
            final int keyCode = ShuttleXpressDevice.KeyCodes.ALL_KEYS[i];

            // Add the categories to the screen as we come to them
            if (keyCode == ShuttleXpressDevice.KeyCodes.BUTTON_KEYS[0]) {
                buttonCategory = new PreferenceCategory(getActivity());
                buttonCategory.setTitle("Buttons");
                screen.addPreference(buttonCategory);

            } else if (keyCode == ShuttleXpressDevice.KeyCodes.WHEEL_KEYS[0]) {
                wheelCategory = new PreferenceCategory(getActivity());
                wheelCategory.setTitle("Wheel");
                screen.addPreference(wheelCategory);

            } else if (keyCode == ShuttleXpressDevice.KeyCodes.RING_KEYS[0]) {
                ringCategory = new PreferenceCategory(getActivity());
                ringCategory.setTitle("Ring");
                screen.addPreference(ringCategory);
            }

            Preference keyPreference = new Preference(getActivity());
            keyPreference.setTitle(DeviceInputManager.getNameForDeviceKey(getActivity(), keyCode));
            keyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    launchKeySetDialog(keyCode);
                    return true;
                }
            });
            deviceKeyPreferences[i] = keyPreference;
            screen.addPreference(keyPreference);
        }

        setPreferenceScreen(screen);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}