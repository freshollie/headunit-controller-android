package com.freshollie.headunitcontroller.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.services.ControllerStartupService;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences =
                getSharedPreferences(getString(R.string.PREFERENCES_KEY), Context.MODE_PRIVATE);
        setupToggles();

    }

    public void startMainService(String action) {
        startService(new Intent(getApplicationContext(), ControllerStartupService.class).setAction(action));
    }

    public void setupToggles() {
        Boolean powerOn =
                sharedPreferences.getBoolean(getString(R.string.POWER_ON_DEBUG_KEY), false);
        Boolean debug =
                sharedPreferences.getBoolean(getString(R.string.DEBUG_ENABLED_KEY), false);

        CheckBox debugEnabledCheckbox = (CheckBox) findViewById(R.id.powerDebugEnabled);
        debugEnabledCheckbox.setOnCheckedChangeListener(null);
        debugEnabledCheckbox.setChecked(debug);

        ToggleButton powerOnButton = (ToggleButton) findViewById(R.id.powerToggle);
        powerOnButton.setOnCheckedChangeListener(null);
        powerOnButton.setChecked(powerOn);

        debugEnabledCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.DEBUG_ENABLED_KEY), isChecked);
                editor.apply();

                Log.v(TAG, "Debug enabled: " + String.valueOf(isChecked));

                if (isChecked) {
                    if (sharedPreferences.getBoolean(getString(R.string.POWER_ON_DEBUG_KEY), false)){
                        startMainService(Intent.ACTION_POWER_CONNECTED);
                    } else {
                        startMainService(Intent.ACTION_POWER_DISCONNECTED);
                    }
                }
            }
        });

        powerOnButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v(TAG, "Debug power: " + String.valueOf(isChecked));

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.POWER_ON_DEBUG_KEY), isChecked);
                editor.apply();

                if (sharedPreferences.getBoolean(getString(R.string.DEBUG_ENABLED_KEY), false)) {
                    if (isChecked) {
                        startMainService(Intent.ACTION_POWER_CONNECTED);
                    } else {
                        startMainService(Intent.ACTION_POWER_DISCONNECTED);
                    }
                }
            }
        });



    }
}
