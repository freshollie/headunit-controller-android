package com.freshollie.headunitcontroller.ui.settings.fragments;

/**
 * Created by freshollie on 03.12.17.
 */

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.SettingsActivity;

/**
 * This fragment shows the on suspend preferences of the controller
 */
public class SuspendPreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // All the preferences are in the XML
        addPreferencesFromResource(R.xml.pref_suspend);
        setHasOptionsMenu(true);
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