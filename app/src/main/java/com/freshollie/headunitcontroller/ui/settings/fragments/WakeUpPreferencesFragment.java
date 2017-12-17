package com.freshollie.headunitcontroller.ui.settings.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.SettingsActivity;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by freshollie on 03.12.17.
 */

public class WakeUpPreferencesFragment extends PreferenceFragment {
    private static final String TAG = WakeUpPreferencesFragment.class.getSimpleName();

    private ListPreference maxDevicesPreference;
    private EditTextPreference routineDelayPreference;
    private SwitchPreference setVolumeSwitchPreference;
    private ListPreference volumeLevelPreference;
    private Preference shellCommandsPreference;
    private Preference bluetoothTetherPreference;
    private ListPreference screenRotationPreference;

    private SharedPreferences sharedPreferences;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_wakeup);
        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setupScreen();

    }

    private void setupScreen() {
        screenRotationPreference = (ListPreference)
                findPreference(getString(R.string.pref_screen_orientation_key));

        maxDevicesPreference = (ListPreference)
                findPreference(getString(R.string.pref_num_devices_key));

        routineDelayPreference = (EditTextPreference)
                findPreference(getString(R.string.pref_wake_up_delay_key));

        setVolumeSwitchPreference = (SwitchPreference)
                findPreference(getString(R.string.pref_set_volume_key));

        volumeLevelPreference = (ListPreference)
                findPreference(getString(R.string.pref_volume_level_key));

        shellCommandsPreference = findPreference(getString(R.string.pref_shell_wakeup_commands_key));

        bluetoothTetherPreference = findPreference(getString(R.string.pref_bluetooth_tether_address));

        String[] rotationId = new String[5];
        for (int i = 0; i < 5; i++) {
            rotationId[i] = String.valueOf(i);
        }

        final String[] degrees = new String[5];
        for (int i = 0; i < 4; i++) {
            degrees[i + 1] = String.valueOf(i * 90);
        }

        degrees[0] = getString(R.string.pref_screen_orientation_auto_entry);

        screenRotationPreference.setEntries(degrees);
        screenRotationPreference.setEntryValues(rotationId);

        screenRotationPreference.setSummary(
                getString(
                        R.string.pref_screen_orientation_summary,
                        degrees[Integer.valueOf(screenRotationPreference.getValue())]
                )
        );

        screenRotationPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        preference.setSummary(
                                getString(
                                        R.string.pref_screen_orientation_summary,
                                        degrees[Integer.valueOf((String) o)]
                                )
                        );

                        return true;
                    }
                }
        );

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


        // Summary is automatically updated
        maxDevicesPreference.setDefaultValue(3);
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

        volumeLevelPreference.setSummary(
                getString(R.string.pref_volume_level_summary, volumeLevelPreference.getValue())
        );

        volumeLevelPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        preference.setSummary(getString(R.string.pref_volume_level_summary, o));
                        return true;
                    }
                }
        );

        bluetoothTetherPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showBluetoothDevicesDialog();
                return false;
            }
        });

        updateBluetoothTetherSummary();
    }

    private void updateBluetoothTetherSummary() {
        String savedAddress =
                sharedPreferences.getString(
                        getString(R.string.pref_bluetooth_tether_address),
                        ""
                );

        BluetoothDevice device = null;

        if (bluetoothAdapter != null && !savedAddress.isEmpty()) {
            device = bluetoothAdapter.getRemoteDevice(savedAddress);
        }

        String deviceName = getActivity().getString(R.string.pref_bluetooth_tether_no_device_set);
        if (device != null) {
            deviceName = device.getName();
        } else if (!savedAddress.isEmpty()) {
            // The previously saved devices has unpaired
            sharedPreferences
                    .edit()
                    .putString(getString(R.string.pref_bluetooth_tether_address), "")
                    .apply();
        }

        bluetoothTetherPreference.setSummary(
                getString(R.string.pref_bluetooth_tether_address_summary, deviceName)
        );
    }

    private void showBluetoothDevicesDialog() {
        String currentSelectedDeviceAddress = sharedPreferences.getString(
                getString(R.string.pref_bluetooth_tether_address),
                ""
        );

        int currentDeviceIndex = -1;
        Set<BluetoothDevice> bluetoothDevices = null;

        if (bluetoothAdapter != null) {
            bluetoothDevices = bluetoothAdapter.getBondedDevices();
        }

        if (bluetoothDevices != null && !bluetoothDevices.isEmpty()) {

            final String[] deviceAddresses = new String[bluetoothDevices.size() + 1];
            String[] deviceNames = new String[bluetoothDevices.size() + 1];
            // Show the None string for the first device
            deviceNames[0] = getString(R.string.bluetooth_devices_none_option);

            int i = 0;
            for (BluetoothDevice device: bluetoothDevices) {
                String name = device.getName();
                if (name == null) {
                    name = device.getAddress();
                }

                if (name != null) {
                    deviceNames[i + 1] = device.getName();
                    deviceAddresses[i + 1] = device.getAddress();
                    if (device.getAddress().equals(currentSelectedDeviceAddress)) {
                        currentDeviceIndex = i + 1;
                    }
                }

                i++;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_bluetooth_tether_address_select_title)
                    .setSingleChoiceItems(
                            deviceNames,
                            currentDeviceIndex,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String address = "";
                                    if (i > 0) {
                                        address = deviceAddresses[i];
                                    }
                                    sharedPreferences
                                            .edit()
                                            .putString(
                                                    getString(R.string.pref_bluetooth_tether_address),
                                                    address
                                            )
                                            .apply();
                                    updateBluetoothTetherSummary();
                                    dialogInterface.dismiss();
                                }
                            })
                    .setPositiveButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.pref_bluetooth_no_devices_messaged)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
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