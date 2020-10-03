package com.skywire.skycoin.vpn;

import android.app.NotificationManager;
import android.content.Context;

/**
 * Constant values used in various parts of the app and some helper functions related to them.
 */
public class Globals {
    /**
     * Address of the local Skywire node.
     */
    public static final String LOCAL_VISOR_ADDRESS = "localhost";
    /**
     * Port of the local Skywire node.
     */
    public static final int LOCAL_VISOR_PORT = 7890;

    /**
     * ID of the notification channel for showing the VPN service status.
     */
    public static final String NOTIFICATION_CHANNEL_ID = "SkywireVPN";
    /**
     * ID of the notification channel for showing alerts and errors.
     */
    public static final String ALERT_NOTIFICATION_CHANNEL_ID = "SkywireVPNAlerts";

    /**
     * ID of the VPN service status notification.
     */
    public static final int SERVICE_STATUS_NOTIFICATION_ID = 1;
    /**
     * ID of the notification for informing about errors while trying to automatically start the
     * VPN service during boot.
     */
    public static final int AUTOSTART_ALERT_NOTIFICATION_ID = 10;
    /**
     * ID of the generic error notifications.
     */
    public static final int ERROR_NOTIFICATION_ID = 50;

    /**
     * Addresses used for checking if the device has internet connectivity. Any number of
     * addresses, but at least 1, can be used. Addresses will be checked sequentially and only
     * until being able to connect with one.
     */
    public static final String[] INTERNET_CHECKING_ADDRESSES = new String[]{"https://dmsg.discovery.skywire.skycoin.com", "https://www.skycoin.com"};

    /**
     * Closes all the alert and error notifications created by the app.
     */
    public static void removeAllAlertNotifications() {
        NotificationManager notificationManager = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(Globals.AUTOSTART_ALERT_NOTIFICATION_ID);
        notificationManager.cancel(Globals.ERROR_NOTIFICATION_ID);
    }

    /**
     * List with all the possible app selection modes. Each option has an associated string value.
     */
    public enum AppFilteringModes {
        /**
         * All apps must be protected by the VPN service, no matter which apps have been selected
         * by the user.
         */
        PROTECT_ALL("PROTECT_ALL"),
        /**
         * Only the apps selected by the user must be protected by the VPN service.
         */
        PROTECT_SELECTED("PROTECT_SELECTED"),
        /**
         * Apps selected by the user must NOT be protected by the VPN service. All other apps
         * must be protected.
         */
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
