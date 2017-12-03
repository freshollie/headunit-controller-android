package com.freshollie.headunitcontroller.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.ui.SettingsActivity;

import java.util.Arrays;

/**
 * Created by freshollie on 03.09.17.
 */

public class LauncherWidgetProvider extends AppWidgetProvider {
    static String VOICE_INTENT_PACKAGE = "voice";

    static String[] launcherApps = {
            "com.android.chrome",
            "au.com.shiftyjelly.pocketcasts",
            "com.google.android.apps.maps",
            "com.freshollie.monkeyboard.keystoneradio",
            VOICE_INTENT_PACKAGE,
            "com.spotify.music"
    };

    static String ACTION_LAUNCH_APP =
            "com.freshollie.headunitcontroller.widget.laucherwidgetprovider.action_launch_app";

    static int[] launcherButtons = {
            R.id.chrome_icon,
            R.id.pocketcasts_icon,
            R.id.maps_icon,
            R.id.radio_icon,
            R.id.voicesearch_icon,
            R.id.spotify_icon
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId: appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.layout_launcher_widget);

            // Add the intents for the buttons
            for (int i = 0; i < launcherApps.length; i++) {
                String packageName = launcherApps[i];
                int buttonId = launcherButtons[i];
                remoteViews.setOnClickPendingIntent(buttonId,
                        getLaunchIntent(context, packageName));
            }

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private PendingIntent getLaunchIntent(Context context, String packageName) {
        Intent i;

        // Voice app needs a different intent
        if (!packageName.equals(VOICE_INTENT_PACKAGE)) {
            i = context.getPackageManager().getLaunchIntentForPackage(packageName);
        } else {
            i = new Intent("android.intent.action.VOICE_ASSIST")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if (i != null) {
            return PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return null;
    }
}