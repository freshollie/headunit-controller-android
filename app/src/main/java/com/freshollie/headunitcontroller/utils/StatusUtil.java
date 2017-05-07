package com.freshollie.headunitcontroller.utils;

import android.text.TextUtils;

import java.util.ArrayList;

/**
 * Created by freshollie on 26/04/17.
 */

public class StatusUtil {
    private static StatusUtil INSTANCE = new StatusUtil();

    private ArrayList<OnStatusChangeListener> statusChangeListeners = new ArrayList<>();

    private ArrayList<String> log = new ArrayList<>();

    private String status;

    public interface OnStatusChangeListener {
        void onStatusChange(String status);
    }

    private StatusUtil() {

    }

    public String getHistory() {
        return TextUtils.join("\n", log);
    }

    public static StatusUtil getInstance() {
        return INSTANCE;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String newStatus) {
        status = newStatus;
        log.add(status);

        if (log.size() > 100) {
            log.remove(0);
        }

        for (OnStatusChangeListener statusChangeListener : statusChangeListeners) {
            statusChangeListener.onStatusChange(newStatus);
        }
    }

    public void addOnStatusChangeListener(OnStatusChangeListener listener) {
        statusChangeListeners.add(listener);
    }

    public void removeOnStatusChangeListener(OnStatusChangeListener listener) {
        statusChangeListeners.remove(listener);
    }
}
