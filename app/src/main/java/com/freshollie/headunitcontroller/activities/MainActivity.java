package com.freshollie.headunitcontroller.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.services.MainService;
import com.freshollie.headunitcontroller.utils.PowerUtil;

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
        setupMaxDevicesInput();

        if (!sharedPreferences.getBoolean(getString(R.string.DEBUG_ENABLED_KEY), false)) {
            if (PowerUtil.isConnected(getApplicationContext())) {
                startMainService(Intent.ACTION_POWER_CONNECTED);
            }
        }
    }

    public void startMainService(String action) {
        startService(new Intent(getApplicationContext(), MainService.class).setAction(action));
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

    public void setupMaxDevicesInput(){
        EditText maxDevicesInput = (EditText) findViewById(R.id.num_devices_input);
        maxDevicesInput.setText(
                String.valueOf(
                        sharedPreferences.getInt(
                                getString(R.string.NUM_DEVICES_KEY), 0)
                )
        );
        maxDevicesInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().equals("")) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt(getString(R.string.NUM_DEVICES_KEY), Integer.valueOf(charSequence.toString()));
                    editor.apply();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
}
