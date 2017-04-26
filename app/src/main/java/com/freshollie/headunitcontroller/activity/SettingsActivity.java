package com.freshollie.headunitcontroller.activity;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.service.MainService;
import com.freshollie.headunitcontroller.utils.PowerUtil;
import com.freshollie.headunitcontroller.utils.StatusUtil;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements StatusUtil.OnStatusChangeListener {

    private String TAG = SettingsActivity.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    private StatusUtil statusUtil;

    private static boolean isLargeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void startMainService(Context context, String action) {
        context.startService(new Intent(context, MainService.class).setAction(action));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        statusUtil = StatusUtil.getInstance();

        if (!sharedPreferences.getBoolean(getString(R.string.pref_debug_enabled_key), false)) {
            if (PowerUtil.isConnected(getApplicationContext())) {
                startMainService(this, Intent.ACTION_POWER_CONNECTED);
            }
        }
    }

    @Override
    public void onResume() {
        statusUtil.addOnStatusChangeListener(this);
        onStatusChange(statusUtil.getStatus());

        super.onResume();
    }

    @Override
    public void onPause() {
        statusUtil.removeOnStatusChangeListener(this);

        super.onPause();
    }

    @Override
    public void onStatusChange(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    String titleString = getString(R.string.title_activity_settings) + " - " +
                            newStatus;

                    if (newStatus == null) {
                        titleString = getString(R.string.title_activity_settings);
                    }

                    actionBar.setTitle(titleString);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isLargeScreen(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || WakeUpPreferencesFragment.class.getName().equals(fragmentName)
                || SuspendPreferencesFragment.class.getName().equals(fragmentName)
                || DebuggingFragment.class.getName().equals(fragmentName)
                || InputPreferencesFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WakeUpPreferencesFragment extends PreferenceFragment {

        ListPreference maxDevicesPreference;
        EditTextPreference routineDelayPreference;
        SwitchPreference setVolumeSwitchPreference;
        ListPreference volumeLevelPreference;
        Preference shellCommandsPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wakeup);

            setHasOptionsMenu(true);

            maxDevicesPreference = (ListPreference)
                    findPreference(getString(R.string.pref_num_devices_key));

            routineDelayPreference = (EditTextPreference)
                    findPreference(getString(R.string.pref_wake_up_delay_key));

            setVolumeSwitchPreference = (SwitchPreference)
                    findPreference(getString(R.string.pref_set_volume_key));

            volumeLevelPreference = (ListPreference)
                    findPreference(getString(R.string.pref_volume_level_key));

            shellCommandsPreference = findPreference(getString(R.string.pref_shell_commands_key));

            setupPreferences();

        }

        private void setupPreferences() {

            routineDelayPreference.setSummary(getString(R.string.pref_wake_up_delay_summary,
                    routineDelayPreference.getText()));

            routineDelayPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.setSummary(getString(R.string.pref_wake_up_delay_summary, o));

                            View view = getActivity().getCurrentFocus();
                            if (view != null) {
                                InputMethodManager inputManager = (InputMethodManager)
                                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                                        InputMethodManager.HIDE_NOT_ALWAYS);
                            }

                            return true;
                        }
                    }
            );

            String[] numDevicesValues = new String[101];

            for (int i = 0; i < 101; i++) {
                numDevicesValues[i] = String.valueOf(i);
            }

            maxDevicesPreference.setEntries(numDevicesValues);
            maxDevicesPreference.setEntryValues(numDevicesValues);

            setVolumeSwitchPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            boolean newValue = (boolean) o;
                            volumeLevelPreference.setEnabled(newValue);

                            return true;
                        }
                    }
            );

            String[] volumes = new String[17];
            for (int i = 0; i < 17; i++) {
                volumes[i] = String.valueOf(i);
            }

            volumeLevelPreference.setEnabled(setVolumeSwitchPreference.isChecked());
            volumeLevelPreference.setEntries(volumes);
            volumeLevelPreference.setEntryValues(volumes);

            volumeLevelPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            preference.setSummary(getString(R.string.pref_volume_level_summary, o));
                            return true;
                        }
                    }
            );



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

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SuspendPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_suspend);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
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


    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class InputPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_input);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

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

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DebuggingFragment extends PreferenceFragment {

        SwitchPreference debugEnabledToggle;
        SwitchPreference debugPowerToggle;
        private String TAG = DebuggingFragment.class.getSimpleName();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_debug);
            setHasOptionsMenu(false);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

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

                    Log.v(TAG, "Debug enabled: " + String.valueOf(switchValue));

                    if (switchValue) {
                        if (debugPowerToggle.isChecked()) {
                            startMainService(getActivity(), Intent.ACTION_POWER_CONNECTED);
                        } else {
                            startMainService(getActivity(), Intent.ACTION_POWER_DISCONNECTED);
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

                    Log.v(TAG, "Debug power: " + String.valueOf(switchValue));

                    if (debugEnabledToggle.isChecked()){
                        if (switchValue) {
                            startMainService(getActivity(), Intent.ACTION_POWER_CONNECTED);
                        } else {
                            startMainService(getActivity(), Intent.ACTION_POWER_DISCONNECTED);
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
}
