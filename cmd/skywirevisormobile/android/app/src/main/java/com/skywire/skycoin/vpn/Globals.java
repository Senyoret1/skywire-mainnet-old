package com.skywire.skycoin.vpn;

import android.app.NotificationManager;
import android.content.Context;

public class Globals {
    public static final String NOTIFICATION_CHANNEL_ID = "SkywireVPN";
    public static final String ALERT_NOTIFICATION_CHANNEL_ID = "SkywireVPNAlerts";

    public static final int SERVICE_STATUS_NOTIFICATION_ID = 1;
    public static final int SYSTEM_START_ALERT_NOTIFICATION_ID = 10;
    public static final int AUTOSTART_ALERT_NOTIFICATION_ID = 11;

    public static void removeAllAlertNotifications() {
        NotificationManager notificationManager = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(Globals.SYSTEM_START_ALERT_NOTIFICATION_ID);
        notificationManager.cancel(Globals.AUTOSTART_ALERT_NOTIFICATION_ID);
    }

    public enum AppFilteringModes {
        PROTECT_ALL("PROTECT_ALL"),
        PROTECT_SELECTED("PROTECT_SELECTED"),
        IGNORE_SELECTED("IGNORE_SELECTED");

        private final String val;

        AppFilteringModes(final String val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return val;
        }
    }
}
