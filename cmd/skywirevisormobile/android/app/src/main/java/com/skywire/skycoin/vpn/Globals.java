package com.skywire.skycoin.vpn;

public class Globals {
    public static final String NOTIFICATION_CHANNEL_ID = "SkywireVPN";

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
