package com.skywire.skycoin.vpn.helpers;

import android.widget.Toast;

import skywiremob.Skywiremob;

public class HelperFunctions {
    public static void logError(String prefix, Exception e) {
        String errorMsg = prefix + ": " + e.getMessage() + "\n";
        errorMsg += e.toString() + "\n";

        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace)
        {
            errorMsg += stackTraceElement.toString() + "\n";
        }

        Skywiremob.printString(errorMsg);
    }

    public static void showToast(String text, boolean shortDuration) {
        Toast toast = Toast.makeText(App.getContext(), text, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toast.show();
    }
}
