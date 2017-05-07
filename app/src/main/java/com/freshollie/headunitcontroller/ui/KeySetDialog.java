package com.freshollie.headunitcontroller.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.input.DeviceInputManager;
import com.freshollie.headunitcontroller.input.DeviceKeyMapper;
import com.freshollie.shuttlexpressdriver.ShuttleXpressDevice;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by freshollie on 28/04/17.
 */

public class KeySetDialog extends DialogFragment {

    private String TAG = this.getClass().getSimpleName();

    private DeviceKeyMapper keyMapper;
    private int key = ShuttleXpressDevice.KeyCodes.BUTTON_0;

    private DeviceKeyMapper.ActionMap pressAction;
    private DeviceKeyMapper.ActionMap holdAction;

    private Spinner pressActionSpinner;
    private EditText pressExtraEditText;

    private EditText holdDelayEditText;
    private Spinner holdActionSpinner;
    private EditText holdExtraEditText;
    private KeySetDismissListener dismissListener;

    public interface KeySetDismissListener {
        void onDismissed();
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void setOnDismissListener(KeySetDismissListener listener) {
        dismissListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        keyMapper = new DeviceKeyMapper(getActivity());

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setTitle(DeviceInputManager.getNameForDeviceKey(getActivity(), key))
                .setPositiveButton("Save", null)
                .setNegativeButton(android.R.string.cancel, null);

        LayoutInflater i = getActivity().getLayoutInflater();


        pressAction = keyMapper.getKeyPressAction(key);
        holdAction = keyMapper.getKeyHoldAction(key);


        @SuppressLint("InflateParams")
        View v = i.inflate(R.layout.keyset_dialog_fragment, null);

        setDetails(v);

        b.setView(v);

        return b.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AlertDialog) getDialog())
                .getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (saveChanges()) {
                            dismiss();
                        }
                    }
        });

    }

    public HashMap<String, String> getInstalledPackages() {
        List<PackageInfo> packageInfos = getActivity().getPackageManager().getInstalledPackages(0);
        HashMap<String, String> packageNames = new HashMap<>();

        for (PackageInfo info: packageInfos) {
            packageNames.put(
                    info.packageName,
                    info.applicationInfo.loadLabel(getActivity().getPackageManager()).toString()
            );
        }

        return packageNames;
    }

    public void setDetails(View v) {
        pressActionSpinner = (Spinner) v.findViewById(R.id.press_action_spinner);
        pressExtraEditText = (EditText) v.findViewById(R.id.press_extra_input);

        holdActionSpinner = (Spinner) v.findViewById(R.id.hold_action_spinner);
        holdDelayEditText = (EditText) v.findViewById(R.id.hold_delay_input);
        holdExtraEditText = (EditText) v.findViewById(R.id.hold_extra_input);


        String[] actionStrings = new String[DeviceInputManager.ACTIONS.length];

        int i = 0;

        for (String action: DeviceInputManager.ACTIONS) {
            actionStrings[i] = DeviceInputManager.getStringForAction(getActivity(), action);
            i++;
        }

        pressActionSpinner.setAdapter(
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line,
                        actionStrings));
        pressActionSpinner.setSelection(pressAction.getActionId());

        pressActionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (pressAction.getActionId() != i) {
                    pressAction = new DeviceKeyMapper.ActionMap(i, null);
                    String extraPlacement = "";
                    holdExtraEditText.setEnabled(true);

                    if (pressAction.getAction()
                            .equals(DeviceInputManager.ACTION_LAUNCH_APP)) {
                        extraPlacement = getString(R.string.select_application_holder);

                    } else if (pressAction.getAction()
                            .equals(DeviceInputManager.ACTION_SEND_KEYEVENT)) {
                        extraPlacement = getString(R.string.select_key_holder);

                    } else {
                        holdExtraEditText.setEnabled(false);

                    }

                    pressExtraEditText.setText(extraPlacement);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        pressExtraEditText.setText(pressAction.getReadableExtra(getActivity()));

        pressExtraEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pressAction.getAction().equals(DeviceInputManager.ACTION_LAUNCH_APP)) {
                    showSelectAppDialog(pressExtraEditText, pressAction);
                } else if (pressAction.getAction().equals(DeviceInputManager.ACTION_SEND_KEYEVENT)){
                    showKeySelectDialog(pressExtraEditText, pressAction);
                }
            }
        });

        holdDelayEditText.setText(String.valueOf(keyMapper.getKeyHoldDelay(key)));

        holdActionSpinner.setAdapter(
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line,
                        actionStrings));
        holdActionSpinner.setSelection(holdAction.getActionId());

        holdActionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (holdAction.getActionId() != i) {
                    holdAction = new DeviceKeyMapper.ActionMap(i, null);
                    holdExtraEditText.setEnabled(true);
                    String extraPlacement = "";

                    if (holdAction.getAction()
                            .equals(DeviceInputManager.ACTION_LAUNCH_APP)) {
                        extraPlacement = getString(R.string.select_application_holder);

                    } else if (holdAction.getAction()
                            .equals(DeviceInputManager.ACTION_SEND_KEYEVENT)) {
                        extraPlacement = getString(R.string.select_key_holder);

                    } else {
                        holdExtraEditText.setEnabled(false);

                    }

                    holdExtraEditText.setText(extraPlacement);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        holdExtraEditText.setText(holdAction.getReadableExtra(getActivity()));
        holdExtraEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holdAction.getAction().equals(DeviceInputManager.ACTION_LAUNCH_APP)) {
                    showSelectAppDialog(holdExtraEditText, holdAction);
                } else if (holdAction.getAction().equals(DeviceInputManager.ACTION_SEND_KEYEVENT)){
                    showKeySelectDialog(holdExtraEditText, holdAction);
                }
            }
        });

    }

    public void showKeySelectDialog(final EditText resultHolder, final DeviceKeyMapper.ActionMap editMap) {
        String[] allKeyCodes = new String[KeyEvent.getMaxKeyCode()];

        for (int i = 0; i < KeyEvent.getMaxKeyCode(); i++) {
            allKeyCodes[i] = KeyEvent.keyCodeToString(i);
        }

        int selectedKeycode = -1;

        if (editMap.getExtra() != null) {
            selectedKeycode = Integer.parseInt(editMap.getExtra());
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_key_title)
                .setSingleChoiceItems(allKeyCodes, selectedKeycode,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                editMap.setExtra(String.valueOf(i));
                                resultHolder.setText(editMap.getReadableExtra(getActivity()));
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
    }

    public void showSelectAppDialog(final EditText resultHolder, final DeviceKeyMapper.ActionMap editMap) {
        final HashMap<String, String> packages = getInstalledPackages();

        final String[] packageNames = packages.keySet()
                .toArray(new String[packages.keySet().toArray().length]);

        String[] appNames = packages.values()
                .toArray(new String[packages.values().size()]);

        Arrays.sort(appNames, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareTo(t1);
            }
        });

        Arrays.sort(packageNames, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return packages.get(s).compareTo(packages.get(t1));
            }
        });

        int packageNum = -1;

        for (int i = 0; i < packageNames.length; i++) {
            if (packageNames[i].equals(holdAction.getExtra())) {
                packageNum = i;
                break;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_application_title)
                .setSingleChoiceItems(appNames, packageNum,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                editMap.setExtra(packageNames[i]);

                                resultHolder.setText(editMap.getReadableExtra(getActivity()));
                                dialogInterface.dismiss();
                            }
                        })
                .setPositiveButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
    }

    private boolean saveChanges() {
        String dialogText = "";

        if ((pressAction.getAction().equals(DeviceInputManager.ACTION_LAUNCH_APP) &&
                pressAction.getExtra() == null) ||
                (holdAction.getAction().equals(DeviceInputManager.ACTION_LAUNCH_APP) &&
                holdAction.getExtra() == null)) {
            dialogText = getString(R.string.error_no_app_selected);

        } else if ((pressAction.getAction().equals(DeviceInputManager.ACTION_SEND_KEYEVENT) &&
                pressAction.getExtra() == null) ||
                (holdAction.getAction().equals(DeviceInputManager.ACTION_SEND_KEYEVENT) &&
                        holdAction.getExtra() == null)) {
            dialogText = getString(R.string.error_no_key_selected);
        }

        if (!dialogText.isEmpty()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.incomplete_title)
                    .setMessage(dialogText)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
            return false;
        }

        int holdDelay = 0;

        if (!holdDelayEditText.getText().toString().isEmpty()) {
            holdDelay = Integer.parseInt(holdDelayEditText.getText().toString());
            if (holdDelay < 0) {
                holdDelay = 0;
            }
        }

        keyMapper.setKeyAction(key, pressAction.getActionId(), pressAction.getExtra());
        keyMapper.setKeyAction(
                key,
                holdAction.getActionId(),
                holdAction.getExtra(),
                true,
                holdDelay
        );

        return true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if(dismissListener != null) {
            dismissListener.onDismissed();
        }

    }
}
