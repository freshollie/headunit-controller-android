package com.freshollie.headunitcontroller;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

/**
 * Created by freshollie on 17.12.17.
 *
 * Used to set night mode on
 */

public class HeadunitControllerApplication extends Application{
    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }
}
