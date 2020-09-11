package com.skywire.skycoin.vpn;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.skywire.skycoin.vpn.helpers.App;

public class VPNPersistentData {
    private static final String SERVER_PK = "serverPK";
    private static final String SERVER_PASSWORD = "serverPass";
    private static final String LAST_ERROR = "lastError";

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

    public static String getPublicKey(String defaultValue) {
        return settings.getString(SERVER_PK, defaultValue);
    }

    public static String getPassword(String defaultValue) {
        return settings.getString(SERVER_PASSWORD, defaultValue);
    }

    public static String getLastError(String defaultValue) {
        return settings.getString(LAST_ERROR, defaultValue);
    }

    public static void removeLastError() {
        settings.edit().remove(LAST_ERROR).apply();
    }
}
