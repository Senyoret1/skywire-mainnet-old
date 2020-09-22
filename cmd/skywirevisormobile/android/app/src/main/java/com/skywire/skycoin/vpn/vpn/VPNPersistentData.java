package com.skywire.skycoin.vpn.vpn;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.Globals;

import java.util.HashSet;

public class VPNPersistentData {
    private static final String SERVER_PK = "serverPK";
    private static final String SERVER_PASSWORD = "serverPass";
    private static final String LAST_ERROR = "lastError";
    private static final String APPS_SELECTION_MODE = "appsMode";
    private static final String APPS_LIST = "appsList";
    private static final String KILL_SWITCH = "killSwitch";
    private static final String RESTART_VPN = "restartVpn";

    private static final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());

    public static void setPublicKeyAndPassword(String pk, String password) {
        settings
            .edit()
            .putString(SERVER_PK, pk)
            .putString(SERVER_PASSWORD, password)
            .apply();
    }

    public static void setLastError(String val) {
        settings.edit().putString(LAST_ERROR, val).apply();
    }

    public static void setAppsSelectionMode(Globals.AppFilteringModes val) {
        settings.edit().putString(APPS_SELECTION_MODE, val.toString()).apply();
    }

    public static void setAppList(HashSet<String> val) {
        settings.edit().putStringSet(APPS_LIST, val).apply();
    }

    public static void setKillSwitchActivated(boolean val) {
        settings.edit().putBoolean(KILL_SWITCH, val).apply();
    }

    public static void setMustRestartVpn(boolean val) {
        settings.edit().putBoolean(RESTART_VPN, val).apply();
    }

    public static String getPublicKey(String defaultValue) {
        return settings.getString(SERVER_PK, defaultValue);
    }

    public static String getPassword(String defaultValue) {
        return settings.getString(SERVER_PASSWORD, defaultValue);
    }

    public static String getLastError(String defaultValue) {
        return settings.getString(LAST_ERROR, defaultValue);
    }

    public static Globals.AppFilteringModes getAppsSelectionMode() {
        String savedValue = settings.getString(APPS_SELECTION_MODE, null);

        if (savedValue == null || savedValue.equals(Globals.AppFilteringModes.PROTECT_ALL.toString())) {
            return Globals.AppFilteringModes.PROTECT_ALL;
        } else if (savedValue.equals(Globals.AppFilteringModes.PROTECT_SELECTED.toString())) {
            return Globals.AppFilteringModes.PROTECT_SELECTED;
        } else if (savedValue.equals(Globals.AppFilteringModes.IGNORE_SELECTED.toString())) {
            return Globals.AppFilteringModes.IGNORE_SELECTED;
        }

        return Globals.AppFilteringModes.PROTECT_ALL;
    }

    public static HashSet<String> getAppList(HashSet<String> defaultValue) {
        return new HashSet<>(settings.getStringSet(APPS_LIST, defaultValue));
    }

    public static boolean getKillSwitchActivated() {
        return settings.getBoolean(KILL_SWITCH, true);
    }

    public static boolean getMustRestartVpn() {
        return settings.getBoolean(RESTART_VPN, true);
    }

    public static void removeLastError() {
        settings.edit().remove(LAST_ERROR).apply();
    }
}
