package com.skywire.skycoin.vpn;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.skywire.skycoin.vpn.activities.main.MainActivity;
import com.skywire.skycoin.vpn.vpn.VPNCoordinator;

import java.util.HashSet;
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

    public static List<ResolveInfo> getDeviceAppsList() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        return App.getContext().getPackageManager().queryIntentActivities( mainIntent, 0);
    }

    public static HashSet<String> filterAvailableApps(HashSet<String> apps) {
        HashSet<String> availableApps = new HashSet<>();
        for (ResolveInfo app : getDeviceAppsList()) {
            availableApps.add(app.activityInfo.packageName);
        }

        HashSet<String> response = new HashSet<>();
        for (String app : apps) {
            if (availableApps.contains(app)) {
                response.add(app);
            }
        }

        return response;
    }

    public static boolean closeActivityIfServiceRunning(Activity activity) {
        if (VPNCoordinator.getInstance().isServiceRunning()) {
            HelperFunctions.showToast(App.getContext().getString(R.string.vpn_already_running_warning), true);
            activity.finish();

            return true;
        }

        return false;
    }

    public static boolean checkIfNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            } else {
                NetworkCapabilities networkInfo = connectivityManager.getNetworkCapabilities(activeNetwork);
                return networkInfo != null && (
                    networkInfo.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkInfo.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkInfo.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    networkInfo.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                );
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    public static PendingIntent getOpenAppPendingIntent() {
        final Intent openAppIntent = new Intent(App.getContext(), MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openAppIntent.setAction(Intent.ACTION_MAIN);
        openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        return PendingIntent.getActivity(App.getContext(), 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void showAlertNotification(int ID, String title, String content, PendingIntent contentIntent) {
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(content);

        Notification notification = new NotificationCompat.Builder(App.getContext(), Globals.ALERT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(bigTextStyle)
            .setContentIntent(contentIntent)
            .build();

        NotificationManager notificationManager = (NotificationManager)App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ID, notification);
    }
}
