package com.freshollie.headunitcontroller;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * MainService handles main power intents
 * and requesting superuser
 */

public class MainService extends Service {
    public static String TAG = "MainService";
    public static String ACTION_SU_NOT_GRANTED = "com.freshollie.action.su_not_granted";

    private SuperUserManager superUserManager;
    private NotificationHandler notificationHandler;

    public static class PowerAndBootReceiver extends BroadcastReceiver {
        /**
         * Receives all power and boot commands
         * and sends the action to the service
         *
         * @param context application context
         * @param intent the intent to start
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startIntent = new Intent(context, MainService.class);
            startIntent.setAction(intent.getAction());
            // Let the service know why it was started

            context.startService(startIntent);
        }
    }

    @Override
    public void onCreate() {
        superUserManager = SuperUserManager.getInstance();
        notificationHandler = new NotificationHandler(getApplicationContext());

        Log.v(TAG, "Started");

        if (hasListeningPermission()) {
            startMediaMonitor();
        }

    }

    public void startMediaMonitor() {
        startService(new Intent(getApplicationContext(), MediaMonitoringService.class));
    }

    public void informNoListeningPermission() {
        startActivity(
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );

        stopWithStatus(getString(R.string.notify_no_notification_listen_permission));
    }

    public boolean hasListeningPermission() {
        String notificationListenerString =
                Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        //Check notifications access permission
        return !(notificationListenerString == null ||
                !notificationListenerString.contains(getPackageName()));
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent.getAction().equals(ACTION_SU_NOT_GRANTED)) {
            stopWithStatus(getString(R.string.notify_su_not_granted_closing));
            return START_NOT_STICKY;
        }

        if (!hasListeningPermission()) {
            informNoListeningPermission();
            return START_NOT_STICKY;
        }

        if (!superUserManager.hasSuperUserPermission()) {
            superUserManager.request(new SuperUserManager.OnPermissionListener() {
                @Override
                public void onGranted() {
                    Log.v(TAG, "SU permission granted");
                    startService(intent);
                }

                @Override
                public void onDenied() {
                    Log.v(TAG, "SU permission denied");
                    intent.setAction(ACTION_SU_NOT_GRANTED);
                    startService(intent);
                }
            });

        } else {
            if (intent.getAction() != null) {
                Intent routineServiceIntent = new Intent(getApplicationContext(), RoutineService.class);
                routineServiceIntent.setAction(intent.getAction());
                startService(routineServiceIntent);
            }

        }

        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void stopWithStatus(String status){
        Log.v(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStopWithStatus(status);
        stopSelf();
    }

    public void stopWithStatusAndAction(String status, PendingIntent action) {
        Log.v(TAG, "Stopping with status '" + status + "'");
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();
        notificationHandler.notifyStopWithStatusAndAction(status, action);
        stopSelf();
    }

    @Override
    public void onDestroy(){
        stopService(new Intent(this, MediaMonitoringService.class));
        stopService(new Intent(this, RoutineService.class));

    }


}
