package com.skywire.skycoin.vpn.helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.skywire.skycoin.vpn.SkywireVPNService;

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

    public static boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) App.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SkywireVPNService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
