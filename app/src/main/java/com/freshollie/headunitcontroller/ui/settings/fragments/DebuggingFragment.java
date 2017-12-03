package com.freshollie.headunitcontroller.ui.settings.fragments;

/**
 * Created by freshollie on 03.12.17.
 */

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.services.MainService;
import com.freshollie.headunitcontroller.SettingsActivity;

/**
 * This fragment shows the debugging preferences of the controller
 */
public class DebuggingFragment extends PreferenceFragment {
    private static final String TAG = DebuggingFragment.class.getSimpleName();

    SwitchPreference debugEnabledToggle;
    SwitchPreference debugPowerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_debug);
        setHasOptionsMenu(false);

        debugEnabledToggle = (SwitchPreference) findPreference(getString(R.string.pref_debug_enabled_key));
        debugPowerToggle = (SwitchPreference) findPreference(getString(R.string.pref_power_on_debug_key));

        setupToggles();
    }

    public void setupToggles() {
        final Boolean debugEnabled = debugEnabledToggle.isChecked();

        debugPowerToggle.setEnabled(debugEnabled);

        debugEnabledToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean switchValue = (boolean) newValue;

                Log.d(TAG, "Debug enabled: " + String.valueOf(switchValue));

                if (switchValue) {
                    if (debugPowerToggle.isChecked()) {
                        MainService.start(getActivity(), Intent.ACTION_POWER_CONNECTED);
                    } else {
                        MainService.start(getActivity(), Intent.ACTION_POWER_DISCONNECTED);
                    }
                }

                debugPowerToggle.setEnabled(switchValue);

                return true;
            }
        });


        debugPowerToggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean switchValue = (boolean) newValue;

                Log.d(TAG, "Debug power: " + String.valueOf(switchValue));

                if (debugEnabledToggle.isChecked()){
                    if (switchValue) {
                        MainService.start(getActivity(), Intent.ACTION_POWER_CONNECTED);
                    } else {
                        MainService.start(getActivity(), Intent.ACTION_POWER_DISCONNECTED);
                    }
                }

                return true;
            }
        });
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