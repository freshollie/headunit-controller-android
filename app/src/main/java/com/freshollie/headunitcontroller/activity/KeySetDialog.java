package com.freshollie.headunitcontroller.activity;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.input.DeviceInputManager;
import com.freshollie.headunitcontroller.input.DeviceKeyMapper;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

/**
 * Created by freshollie on 28/04/17.
 */

public class KeySetDialog extends DialogFragment {

    private DeviceKeyMapper keyMapper;
    private int key = ShuttleXpressDevice.KeyCodes.BUTTON_0;

    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.keyset_dialog_fragment, container, false);

        keyMapper = new DeviceKeyMapper(getActivity());

        refreshDetails(v);

        return v;
    }

    public void refreshDetails(View v) {

        ((TextView) v.findViewById(R.id.key_title))
                .setText(DeviceInputManager.getNameForDeviceKey(getActivity(), key));

        Spinner pressActionSpinner = (Spinner) v.findViewById(R.id.press_action_spinner);

        String[] actionStrings = new String[DeviceInputManager.ALL_ACTIONS.length];

        int i = 0;

        for (String action: DeviceInputManager.ALL_ACTIONS) {
            actionStrings[i] = DeviceInputManager.getStringForAction(getActivity(), action);
            i++;
        }

        pressActionSpinner.setAdapter(
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line,
                        actionStrings));

        pressActionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                keyMapper.setKeyAction(key, DeviceInputManager.ALL_ACTIONS[i]);
                refreshDetails(getView());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
}
