package com.freshollie.headunitcontroller;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Freshollie on 14/12/2016.
 */

public class NotificationHandler {
    private Context context;
    public static int STATUS_NOTIFICATION = 0;
    public static int SERVICE_NOTIFICATION = 1;

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    public NotificationHandler(Context appContext) {
        context = appContext;
        buildNotification();
        setNotificationManager();
    }

    public void buildNotification() {
        notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext())
                .setContentTitle(context.getString(R.string.app_name))
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(
                        PendingIntent.getActivity(
                                context,
                                0,
                                new Intent(context, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                );
    }

    public void setNotificationManager() {
        notificationManager = (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
    }

    public Notification notifyStatus(String status) {
        Notification notification = notificationBuilder.setContentText(status).build();
        notificationManager.notify(SERVICE_NOTIFICATION, notification);
        return notification;
    }

    public void notifyStopWithStatus(String status) {
        Notification notification = notificationBuilder
                .setContentText(context.getString(R.string.notify_su_not_granted_closing))
                .setOngoing(false)
                .build();

        notificationManager.notify(STATUS_NOTIFICATION, notification);
    }

    public void cancel(int id) {
        notificationManager.cancel(id);
    }
}