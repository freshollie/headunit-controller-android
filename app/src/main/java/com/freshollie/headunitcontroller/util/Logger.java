package com.freshollie.headunitcontroller.util;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by freshollie on 26/04/17.
 */

public class Logger {
    private static Logger INSTANCE = new Logger();

    private ArrayList<OnNewLogLineListener> newLogLineListeners = new ArrayList<>();

    private ArrayList<String> log = new ArrayList<>();

    private String lastLine;

    public interface OnNewLogLineListener {
        void onNewLine(String newLine);
    }

    private Logger() {

    }

    public String getJoinedLog() {
        return TextUtils.join("\n", log);
    }

    public static Logger getInstance() {
        return INSTANCE;
    }

    public String getLastLogLine() {
        return lastLine;
    }

    public void newLogLine(String newLine) {
        lastLine = newLine;
        log.add(lastLine);

        if (log.size() > 100) {
            log.remove(0);
        }

        for (OnNewLogLineListener newLogLineListener : newLogLineListeners) {
            newLogLineListener.onNewLine(newLine);
        }
    }

    public void registerOnNewLineListener(OnNewLogLineListener listener) {
        newLogLineListeners.add(listener);
    }

    public void removeOnNewLineListener(OnNewLogLineListener listener) {
        newLogLineListeners.remove(listener);
    }

    public static void log(String TAG, String message) {
        Log.d(TAG, message);
        getInstance().newLogLine(message);
    }
}
