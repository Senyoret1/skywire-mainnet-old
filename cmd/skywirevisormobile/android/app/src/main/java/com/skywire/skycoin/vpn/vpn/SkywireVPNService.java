package com.skywire.skycoin.vpn.vpn;;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import androidx.core.app.NotificationCompat;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.Globals;
import com.skywire.skycoin.vpn.HelperFunctions;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import skywiremob.Skywiremob;

import static com.skywire.skycoin.vpn.vpn.VPNStates.getTextForState;

public class SkywireVPNService extends VpnService {
    public static final String ACTION_CONNECT = "com.skywire.android.vpn.START";
    public static final String ACTION_DISCONNECT = "com.skywire.android.vpn.STOP";

    public static final String ERROR_MSG_PARAM = "ErrorMsg";
    public static final String STARTED_BY_THE_SYSTEM_PARAM = "StartedByTheSystem";
    public static final String STOP_REQUESTED_PARAM = "StopRequested";

    public static int lastInstanceID = 0;
    public int instanceID = 0;

    private NotificationManager notificationManager = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

    private Messenger messenger;

    private VPNRunnable vpnRunnable;
    private VPNWorkInterface vpnInterface;

    private int currentState = VPNStates.STARTING;

    private Disposable restartingSubscription;
    private Disposable vpnRunnableSubscription;

    private boolean startedByTheSystem = false;
    private boolean stopRequested = false;
    private boolean serviceDestroyed = false;

    private String lastErrorMsg = "";

    private void restart() {
        restartingSubscription = Observable.just(0).delay(1, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(val -> runVpn());
    }

    private void informNetState(int newState) {
        if (lastInstanceID != instanceID) {
            return;
        }

        Message msg = Message.obtain();
        msg.what = newState;

        Bundle dataBundle = new Bundle();
        dataBundle.putBoolean(STARTED_BY_THE_SYSTEM_PARAM, startedByTheSystem);
        dataBundle.putBoolean(STOP_REQUESTED_PARAM, stopRequested);
        if (newState == VPNStates.ERROR || newState == VPNStates.BLOCKING_ERROR) {
            lastErrorMsg = vpnRunnable != null ? vpnRunnable.getLastErrorMsg() : lastErrorMsg;
            dataBundle.putString(ERROR_MSG_PARAM, lastErrorMsg);
        }
        msg.setData(dataBundle);

        if (!App.displayingUI() && currentState != newState) {
            if (!serviceDestroyed && (newState == VPNStates.CONNECTED ||
                newState == VPNStates.RESTORING_VPN ||
                newState == VPNStates.RESTORING_SERVICE ||
                newState == VPNStates.ERROR ||
                newState == VPNStates.BLOCKING_ERROR))
            {
                HelperFunctions.showToast(getString(getTextForState(newState)), false);
            }

            if (newState == VPNStates.DISCONNECTED || newState == VPNStates.DISCONNECTING || newState == VPNStates.OFF) {
                HelperFunctions.showToast(getString(getTextForState(newState)), false);
            }
        }

        currentState = newState;

        try {
            messenger.send(msg);
        } catch (Exception e) { }

        updateForegroundNotification();
    }

    private void updateState(int newState) {
        int processedState = newState;

        if (processedState >= 200 && processedState < 300 && currentState >= 400 && currentState <= 500) {
            processedState = currentState;
        }

        if (processedState >= 300 && processedState < 400) {
            vpnRunnable = null;
            if (vpnRunnableSubscription != null) {
                vpnRunnableSubscription.dispose();
            }
        }

        if (!stopRequested && !serviceDestroyed) {
            if (processedState >= 400 && processedState < 500) {
                if (VPNPersistentData.getMustRestartVpn()) {
                    processedState = VPNStates.RESTORING_SERVICE;
                } else if (processedState == VPNStates.ERROR) {
                    stopRequested = true;
                }
            }

            if (currentState == VPNStates.RESTORING_SERVICE) {
                if (processedState >= 150 && processedState < 300) {
                    processedState = VPNStates.RESTORING_SERVICE;
                }

                if (processedState >= 300 && processedState < 400) {
                    processedState = VPNStates.RESTORING_SERVICE;
                    restart();
                }
            } else {
                if (processedState >= 300 && processedState < 400) {
                    processedState = currentState;
                    finishIfAppropriate();
                }
            }
        } else {
            if (processedState >= 300 && processedState < 400) {
                processedState = currentState;
                finishIfAppropriate();
            }
        }

        informNetState(processedState);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastInstanceID += 1;
        instanceID = lastInstanceID;

        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            stopRequested = true;

            if (vpnRunnable != null) {
                vpnRunnable.disconnect();
            } else {
                finishIfAppropriate();
            }

            updateState(currentState);
        } else {
            if (messenger == null) {
                messenger = VPNCoordinator.getInstance().getCommunicationMessenger();
            }

            if (vpnInterface == null) {
                // Become a foreground service. Background services can be VPN services too, but
                // they can be killed by background check before getting a chance to
                // receive onRevoke().
                makeForeground();

                vpnInterface = new VPNWorkInterface(this);
            }

            if (!vpnInterface.alreadyConfigured() && (VPNPersistentData.getProtectBeforeConnected() || intent == null || !ACTION_CONNECT.equals(intent.getAction()))) {
                try {
                    vpnInterface.configure(VPNWorkInterface.Modes.BLOCKING);
                } catch (Exception e) {
                    HelperFunctions.logError("Configuring VPN work interface before connecting", e);
                    lastErrorMsg = getString(R.string.vpn_service_network_protection_error);
                    updateState(VPNStates.ERROR);
                    finishIfAppropriate();

                    return START_NOT_STICKY;
                }

                if (intent == null || !ACTION_CONNECT.equals(intent.getAction())) {
                    HelperFunctions.showToast(getString(R.string.vpn_service_network_unavailable_warning), false);
                }
            }

            if (intent == null || !ACTION_CONNECT.equals(intent.getAction())) {
                startedByTheSystem = true;
            }
            updateState(currentState);

            runVpn();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Skywiremob.printString("VPN service destroyed.");
        serviceDestroyed = true;

        if (vpnRunnable != null) {
            vpnRunnable.disconnect();
        } else {
            finishIfAppropriate();
        }
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Skywiremob.printString("onRevoke called");
        this.stopSelf();
    }

    private void runVpn() {
        if (vpnRunnable == null) {
            vpnRunnable = new VPNRunnable(this, vpnInterface);
        }

        if (vpnRunnableSubscription != null) {
            vpnRunnableSubscription.dispose();
        }

        vpnRunnableSubscription = vpnRunnable.start().subscribe(state -> updateState(state));
    }

    private void finishIfAppropriate() {
        if (vpnRunnable == null && (vpnInterface == null || !vpnInterface.alreadyConfigured() || stopRequested || serviceDestroyed || currentState < 400 || currentState >= 500 || !VPNPersistentData.getKillSwitchActivated())) {
            if (lastInstanceID == instanceID) {
                if (vpnInterface != null) {
                    vpnInterface.close();

                    // Create another interface and close it immediately to avoid a bug in older Android
                    // versions when the app is added to the ignore list.
                    vpnInterface = new VPNWorkInterface(this);
                    try {
                        vpnInterface.configure(VPNWorkInterface.Modes.DELETING);
                    } catch (Exception e) { }
                    vpnInterface.close();
                }

                notificationManager.cancel(Globals.SERVICE_STATUS_NOTIFICATION_ID);

                Observable.just(0).delay(100, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(val -> updateState(VPNStates.OFF));

                if (!App.displayingUI() && !VPNPersistentData.getKillSwitchActivated() && VPNPersistentData.getLastError(null) != null) {
                    HelperFunctions.showAlertNotification(
                        Globals.ERROR_NOTIFICATION_ID,
                        getString(R.string.general_app_name),
                        getString(R.string.general_connection_error),
                        HelperFunctions.getOpenAppPendingIntent()
                    );
                }
            }

            vpnInterface = null;

            vpnRunnable = null;
            if (vpnRunnableSubscription != null) {
                vpnRunnableSubscription.dispose();
            }
            if (restartingSubscription != null) {
                restartingSubscription.dispose();
            }

            stopForeground(true);
            stopSelf();
        }
    }

    private void updateForegroundNotification() {
        if (!serviceDestroyed) {
            notificationManager.notify(Globals.SERVICE_STATUS_NOTIFICATION_ID, createUpdatedNotification());
        }
    }

    private void makeForeground() {
        startForeground(Globals.SERVICE_STATUS_NOTIFICATION_ID, createUpdatedNotification());
    }

    private Notification createUpdatedNotification() {

        int title = R.string.vpn_service_state_preparing;
        if (currentState == VPNStates.CONNECTED) {
            title = getTextForState(currentState);
        } else {
            if (currentState >= VPNStates.DISCONNECTING) {
                title = R.string.vpn_service_state_finishing;
            } else if (currentState >= VPNStates.RESTORING_VPN && currentState < VPNStates.DISCONNECTING) {
                title = R.string.vpn_service_state_restoring;
            }
        }

        int icon = R.drawable.ic_lines;
        if (vpnInterface != null && vpnInterface.alreadyConfigured()) {
            if (currentState == VPNStates.CONNECTED) {
                icon = R.drawable.ic_filled;
            } else {
                icon = R.drawable.ic_alert;
            }
        }
        if (currentState == VPNStates.ERROR || currentState == VPNStates.BLOCKING_ERROR) {
            icon = R.drawable.ic_error;
        }

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
            .bigText(getString(getTextForState(currentState)))
            .setBigContentTitle(getString(title));

        return new NotificationCompat.Builder(this, Globals.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(getString(title))
            .setContentText(getString(getTextForState(currentState)))
            .setStyle(bigTextStyle)
            .setContentIntent(HelperFunctions.getOpenAppPendingIntent())
            .setOnlyAlertOnce(true)
            .setSound(null)
            .build();
    }
}
