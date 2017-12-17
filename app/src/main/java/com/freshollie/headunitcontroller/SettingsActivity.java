package com.freshollie.headunitcontroller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.freshollie.headunitcontroller.services.MainService;
import com.freshollie.headunitcontroller.ui.settings.AppCompatPreferenceActivity;
import com.freshollie.headunitcontroller.ui.settings.fragments.DebuggingFragment;
import com.freshollie.headunitcontroller.ui.settings.fragments.InfoFragment;
import com.freshollie.headunitcontroller.ui.settings.fragments.InputPreferencesFragment;
import com.freshollie.headunitcontroller.ui.settings.fragments.SuspendPreferencesFragment;
import com.freshollie.headunitcontroller.ui.settings.fragments.WakeUpPreferencesFragment;
import com.freshollie.headunitcontroller.util.PowerUtil;
import com.freshollie.headunitcontroller.util.Logger;

import java.util.List;

/**
 * Settings activity provides a 2 panel settings screen on tablet devices,
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements Logger.OnNewLogLineListener {
    private static String TAG = SettingsActivity.class.getSimpleName();

    private Logger logger;
    private ActionBar actionBar;
    private SharedPreferences sharedPreferences;

    private static boolean isLargeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger = Logger.getInstance();
        actionBar = getSupportActionBar();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onResume() {
        logger.registerOnNewLineListener(this);
        onNewLine(logger.getLastLogLine());
        if (!sharedPreferences.getBoolean(getString(R.string.pref_debug_enabled_key), false)) {
            if (PowerUtil.isConnected(getApplicationContext())) {
                MainService.start(this, Intent.ACTION_POWER_CONNECTED);
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        logger.removeOnNewLineListener(this);
        super.onPause();
    }

    @Override
    public void onNewLine(final String newLine) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (actionBar != null) {
                    String titleString = getString(R.string.title_activity_settings) + " - " +
                            newLine;

                    if (newLine == null) {
                        titleString = getString(R.string.title_activity_settings);
                    }

                    actionBar.setTitle(titleString);
                }
            }
        });
    }

    @Override
    public boolean onIsMultiPane() {
        return isLargeScreen(this);
    }

    @Override
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
                || InfoFragment.class.getName().equals(fragmentName)
                || InputPreferencesFragment.class.getName().equals(fragmentName);
    }
}
