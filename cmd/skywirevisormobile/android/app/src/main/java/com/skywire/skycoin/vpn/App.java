package com.skywire.skycoin.vpn;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.skywire.skycoin.vpn.vpn.VPNCoordinator;

import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class App extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        // Ensure the singleton is initialized early.
        VPNCoordinator.getInstance();

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                Globals.NOTIFICATION_CHANNEL_ID,
                getString(R.string.general_app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(getString(R.string.general_notification_channel_description));
            channel.setSound(null,null);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationChannel alertsChannel = new NotificationChannel(
                    Globals.ALERT_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.general_alert_notification_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription(getString(R.string.general_alert_notification_channel_description));
            notificationManager.createNotificationChannel(alertsChannel);
        }

        RxJavaPlugins.setErrorHandler(throwable -> {
            HelperFunctions.logError("ERROR INSIDE RX: ", throwable);
        });
    }

    public static Context getContext(){
        return appContext;
    }
}
