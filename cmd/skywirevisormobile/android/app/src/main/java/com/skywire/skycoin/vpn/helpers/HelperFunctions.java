package com.skywire.skycoin.vpn.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.List;

import skywiremob.Skywiremob;

public class HelperFunctions {
    public static void logError(String prefix, Throwable e) {
        String errorMsg = prefix + ": " + e.getMessage() + "\n";
        errorMsg += e.toString() + "\n";

        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace)
        {
            errorMsg += stackTraceElement.toString() + "\n";
        }

        Skywiremob.printString(errorMsg);
    }

    public static void logError(String prefix, String errorText) {
        String errorMsg = prefix + ": " + errorText;
        Skywiremob.printString(errorMsg);
    }

    public static void showToast(String text, boolean shortDuration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(App.getContext(), text, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static boolean appIsOnForeground() {
        ActivityManager activityManager = (ActivityManager) App.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appsRunning = activityManager.getRunningAppProcesses();
        if (appsRunning == null) {
            return false;
        }

        final String packageName = App.getContext().getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appsRunning) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }
}
