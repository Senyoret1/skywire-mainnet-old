package com.skywire.skycoin.vpn;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.skywire.skycoin.vpn.vpn.VPNCoordinator;

import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Class for the main app instance.
 */
public class App extends Application {
    /**
     * Reference to the current app instance.
     */
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        // Save the current app instance.
        appContext = this;

        // Ensure the singleton is initialized early.
        VPNCoordinator.getInstance();

        // Create the notification channels, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for the VPN service state updates.
            NotificationChannel stateChannel = new NotificationChannel(
                Globals.NOTIFICATION_CHANNEL_ID,
                getString(R.string.general_app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            );
            stateChannel.setDescription(getString(R.string.general_notification_channel_description));
            stateChannel.setSound(null,null);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(stateChannel);

            // Channel for alerts.
            NotificationChannel alertsChannel = new NotificationChannel(
                    Globals.ALERT_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.general_alert_notification_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription(getString(R.string.general_alert_notification_channel_description));
            notificationManager.createNotificationChannel(alertsChannel);
        }

        // Code for precessing errors which were not caught by the normal error management
        // procedures RxJava has. This prevents the app to be closed by unexpected errors, mainly
        // code trying to report events in closed observables.
        RxJavaPlugins.setErrorHandler(throwable -> {
            HelperFunctions.logError("ERROR INSIDE RX: ", throwable);
        });
    }

    /**
     * Gets the current app context.
     */
    public static Context getContext(){
        return appContext;
    }
}
