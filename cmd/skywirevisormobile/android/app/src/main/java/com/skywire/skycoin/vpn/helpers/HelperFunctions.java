package com.skywire.skycoin.vpn.helpers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.activities.main.MainActivity;
import com.skywire.skycoin.vpn.network.ApiClient;
import com.skywire.skycoin.vpn.vpn.VPNCoordinator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import skywiremob.Skywiremob;

/**
 * General helper functions for different parts of the app.
 */
public class HelperFunctions {
    // Helpers for showing only a max number of decimals.
    public static final DecimalFormat twoDecimalsFormatter = new DecimalFormat("#.##");
    public static final DecimalFormat oneDecimalsFormatter = new DecimalFormat("#.#");
    public static final DecimalFormat zeroDecimalsFormatter = new DecimalFormat("#");

    /**
     * Displays debug information about an error in the console. It includes the several details.
     * @param prefix Text to show before the error details.
     * @param e Error.
     */
    public static void logError(String prefix, Throwable e) {
        // Print the basic error msgs.
        StringBuilder errorMsg = new StringBuilder(prefix + ": " + e.getMessage() + "\n");
        errorMsg.append(e.toString()).append("\n");

        // Print the stack.
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            errorMsg.append(stackTraceElement.toString()).append("\n");
    }

        // Display in the console.
        Skywiremob.printString(errorMsg.toString());
    }

    /**
     * Displays an error msg in the console.
     * @param prefix Text to show before the error msg.
     * @param errorText Error msg.
     */
    public static void logError(String prefix, String errorText) {
        String errorMsg = prefix + ": " + errorText;
        Skywiremob.printString(errorMsg);
    }

    /**
     * Shows a toast notification. Can be used from background threads.
     * @param text Text for the notification.
     * @param shortDuration If the duration of the notification must be short (true) or
     *                      long (false).
     */
    public static void showToast(String text, boolean shortDuration) {
        // Run in the UI thread.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            // Show the notification.
            Toast toast = Toast.makeText(App.getContext(), text, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
            toast.show();
        });
    }

    /**
     * Gets the list of the app launchers installed in the device. More than one entry may share
     * the same package name. The current app is ignored.
     */
    public static List<ResolveInfo> getDeviceAppsList() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        String packageName = App.getContext().getPackageName();
        ArrayList<ResolveInfo> response = new ArrayList<>();

        // Get all the entries in the device which coincide with the intent.
        for (ResolveInfo app : App.getContext().getPackageManager().queryIntentActivities( mainIntent, 0)) {
            if (!app.activityInfo.packageName.equals(packageName)) {
                response.add(app);
            }
        }

        return response;
    }

    /**
     * Filters a list of package names and returns only the ones which are from launchers
     * currently installed in the device. The current app is ignored.
     * @param apps List to filter.
     * @return Filtered list.
     */
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

    /**
     * Closes the provided activity if the VPN service is running. If the activity is closed,
     * a toast is shown.
     * @param activity Activity to close.
     * @return True if the activity was closed, false if not.
     */
    public static boolean closeActivityIfServiceRunning(Activity activity) {
        if (VPNCoordinator.getInstance().isServiceRunning()) {
            HelperFunctions.showToast(App.getContext().getString(R.string.vpn_already_running_warning), true);
            activity.finish();

            return true;
        }

        return false;
    }

    /**
     * Checks if there is connection via internet to at least one of the testing URLs set in the
     * globals class.
     * @param logError If true and there is an error checking the connection, the error will
     *                 be logged.
     * @return Observable which emits if there is connection or not.
     */
    public static Observable<Boolean> checkInternetConnectivity(boolean logError) {
        return checkInternetConnectivity(0, logError);
    }

    /**
     * Internal function for checking if there is internet connectivity, recursively.
     * @param urlIndex Index of the testing URL to check.
     * @param logError If the error, if any, must be logged at the end of the operation.
     */
    private static Observable<Boolean> checkInternetConnectivity(int urlIndex, boolean logError) {
        return ApiClient.checkConnection(Globals.INTERNET_CHECKING_ADDRESSES[urlIndex])
            // If there is a valid response, return true.
            .map(response -> true)
            .onErrorResumeNext(err -> {
                // If there is an error and there are more testing URLs, continue to the next step.
                if (urlIndex < Globals.INTERNET_CHECKING_ADDRESSES.length - 1) {
                    return checkInternetConnectivity(urlIndex + 1, logError);
                }

                if (logError) {
                    HelperFunctions.logError("Checking network connectivity", err);
                }

                return Observable.just(false);
            });
    }

    /**
     * Returns an intent for opening the app.
     */
    public static PendingIntent getOpenAppPendingIntent() {
        final Intent openAppIntent = new Intent(App.getContext(), MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openAppIntent.setAction(Intent.ACTION_MAIN);
        openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        return PendingIntent.getActivity(App.getContext(), 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Process a bytes per second speed and returns the string for displaying it in the UI. The
     * returned speed in in bits and not bytes.
     * @param bytesPerSecondSpeed Speed to process.
     */
    public static String computeSpeedString(long bytesPerSecondSpeed) {
        double current = bytesPerSecondSpeed * 8;
        String[] scales = new String[]{"b/s", "Kb/s", "Mb/s", "Gb/s", "Tb/s"};

        // Divide the speed by 1024 until getting an appropriate scale to return.
        for (int i = 0; i < scales.length - 1; i++) {
            if (current < 1024) {
                // Return decimals depending on how long the number is.
                if (current < 10) {
                    return twoDecimalsFormatter.format(current) + scales[i];
                } else if (current < 100) {
                    return oneDecimalsFormatter.format(current) + scales[i];
                }

                return zeroDecimalsFormatter.format(current) + scales[i];
            }

            current /= 1024;
        }

        return current + scales[scales.length - 1];
    }

    public static String getLatencyValue(double latency, Context ctx) {
        String initialPart;
        String lastPart;

        if (latency >= 1000) {
            initialPart = oneDecimalsFormatter.format(latency / 1000);
            lastPart = ctx.getString(R.string.general_seconds_abbreviation);
        } else {
            initialPart = zeroDecimalsFormatter.format(latency);
            lastPart = ctx.getString(R.string.general_milliseconds_abbreviation);
        }

        return initialPart + lastPart;
    }

    public static int getFlagResourceId(String countryCode) {
        if (countryCode.toLowerCase() != "do") {
            int flagResourceId = App.getContext().getResources().getIdentifier(
                    countryCode.toLowerCase(),
                    "drawable",
                    App.getContext().getPackageName()
            );

            if (flagResourceId != 0) {
                return flagResourceId;
            } else {
                return R.drawable.zz;
            }
        } else {
            return R.drawable.do_flag;
        }
    }
}
