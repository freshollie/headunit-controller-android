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
                .setSmallIcon(R.drawable.ic_tablet_black_24dp)
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

    public void notify(int notificiationId, Notification notification) {
        notificationManager.notify(notificiationId, notification);
    }

    public Notification notifyStatus(String status) {
        Notification notification = notificationBuilder.setContentText(status).build();
        notify(SERVICE_NOTIFICATION, notification);
        return notification;
    }

    public Notification notifyStatus(String status, PendingIntent intent) {
        Notification notification =
                notificationBuilder
                        .setContentText(status)
                        .setContentIntent(intent)
                        .build();

        notify(STATUS_NOTIFICATION, notification);
        return notification;
    }

    public void notifyStopWithStatus(String status) {
        Notification notification = notificationBuilder
                .setContentText(status)
                .setOngoing(false)
                .setAutoCancel(true)
                .build();

        notify(STATUS_NOTIFICATION, notification);
    }

    public void notifyStopWithStatusAndAction(String status, PendingIntent action) {
        Notification notification =
                notificationBuilder
                        .setContentText(status)
                        .setContentIntent(action)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .build();

        notify(STATUS_NOTIFICATION, notification);
    }

    public void cancel(int id) {
        notificationManager.cancel(id);
    }
}