package com.freshollie.headunitcontroller;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class SuperUserManager {
    private DataOutputStream shellInput;
    private Boolean permission = false;

    public static String TAG = "SuperUserManager";

    private static SuperUserManager INSTANCE = new SuperUserManager();

    public interface OnPermissionListener {
        void onGranted();
        void onDenied();
    }

    private SuperUserManager() {
    }

    public static SuperUserManager getInstance() {
        return INSTANCE;
    }

    public boolean execute(final String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su -c " + command);
            int result;

            try {
                result = process.waitFor();

                if(result != 0){ //error executing command
                    Log.d(TAG, "result code : " + result);
                    String line;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    try {
                        while ((line = bufferedReader.readLine()) != null){
                            Log.d(TAG, "Error: " + line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    process.destroy();
                    return true;
                }
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (process != null) {
            process.destroy();
        }

        return false;
    }

    public void request(final OnPermissionListener permissionListener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Requesting SU permission");
                if (execute("ls")) {
                    permission = true;
                    permissionListener.onGranted();
                } else {
                    permission = false;
                    permissionListener.onDenied();
                }
            }
        }).start();
    }

    public boolean hasSuperUserPermission() {
        return permission;
    }

}
