package com.freshollie.headunitcontroller.ui.settings.fragments;

/**
 * Created by freshollie on 03.12.17.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.ui.settings.LogPreference;
import com.freshollie.headunitcontroller.SettingsActivity;
import com.freshollie.headunitcontroller.util.Logger;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
public class InfoFragment extends PreferenceFragment implements Logger.OnNewLogLineListener {
    private String TAG = InfoFragment.class.getSimpleName();

    private Logger logger;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger = Logger.getInstance();

        addPreferencesFromResource(R.xml.pref_info);
        setHasOptionsMenu(true);
        setupResetButton();

    }

    private void setupResetButton() {
        findPreference(getString(R.string.pref_reset_defaults_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(getActivity())
                                .setMessage(
                                        "Are you sure you want to reset all settings to default"
                                )
                                .setNegativeButton(
                                        android.R.string.yes,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                PreferenceManager
                                                        .getDefaultSharedPreferences(getActivity())
                                                        .edit()
                                                        .clear()
                                                        .apply();
                                            }
                                        })
                                .setPositiveButton(
                                        android.R.string.no,
                                        null
                                )
                                .show();
                        return false;
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        logger.registerOnNewLineListener(this);
    }

    private void fillLog() {
        ((LogPreference) findPreference(getString(R.string.pref_log_key)))
                .updateLog(logger.getJoinedLog());
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

    @Override
    public void onNewLine(String newLine) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fillLog();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        logger.removeOnNewLineListener(this);
    }
}