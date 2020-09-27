package com.skywire.skycoin.vpn.vpn;;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import androidx.core.app.NotificationCompat;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.activities.main.MainActivity;
import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.Globals;
import com.skywire.skycoin.vpn.HelperFunctions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.BooleanSupplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import skywiremob.Skywiremob;

import static com.skywire.skycoin.vpn.vpn.VPNStates.getTextForState;

public class SkywireVPNService extends VpnService {
    public static final String ACTION_CONNECT = "com.skywire.android.vpn.START";
    public static final String ACTION_DISCONNECT = "com.skywire.android.vpn.STOP";

    public static final String ERROR_MSG_PARAM = "ErrorMsg";
    public static final String STARTED_BY_THE_SYSTEM_PARAM = "StartedByTheSystem";
    public static final String STOP_REQUESTED_PARAM = "StopRequested";
    public static final String MESSENGER_PARAM = "Messenger";

    private NotificationManager notificationManager = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

    private Messenger messenger;

    private VPNWorkInterface vpnInterface;
    private SkywireVPNConnection connectionRunnable;

    private AtomicInteger mNextConnectionId = new AtomicInteger(1);

    private int currentState = VPNStates.STARTING;
    private String lastErrorMsg;

    private VisorRunnable visor;
    private Disposable checkConnectionSubscription;
    private Disposable visorSubscription;
    private Disposable visorTimeoutSubscription;
    private Disposable vpnConnectionSubscription;

    private boolean alreadyConnected = false;
    private boolean disconnectionStarted = false;
    private boolean disconnectionFinished = false;
    private boolean stopRequested = false;

    private int disconnectionVerifications = 0;

    @Override
    public void onCreate() {
        // Create the intent to "configure" the connection (just start SkywireVPNClient).
        final Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
    }

    private void updateState(int newState) {
        Message msg = Message.obtain();
        msg.what = newState;

        Bundle dataBundle = new Bundle();
        dataBundle.putBoolean(STARTED_BY_THE_SYSTEM_PARAM, false);
        dataBundle.putBoolean(STOP_REQUESTED_PARAM, stopRequested);

        if (newState == VPNStates.CONNECTED) {
            alreadyConnected = true;

            if (!HelperFunctions.appIsOnForeground() && currentState != newState) {
                HelperFunctions.showToast(getString(getTextForState(newState)), false);
            }
        }

        if (newState == VPNStates.RESTORING_VPN && !HelperFunctions.appIsOnForeground() && currentState != newState) {
            HelperFunctions.showToast(getString(getTextForState(newState)), false);
        }

        if (newState == VPNStates.ERROR || newState == VPNStates.BLOCKING_ERROR) {
            dataBundle.putString(ERROR_MSG_PARAM, lastErrorMsg);

            if (!HelperFunctions.appIsOnForeground() && currentState != newState) {
                HelperFunctions.showToast(getString(getTextForState(newState)), false);
            }
        } else if ((newState == VPNStates.DISCONNECTED || newState == VPNStates.DISCONNECTING) && !HelperFunctions.appIsOnForeground()) {
            if (currentState != VPNStates.ERROR && currentState != VPNStates.BLOCKING_ERROR && currentState != newState) {
                HelperFunctions.showToast(getString(getTextForState(newState)), false);
            }
        }

        msg.setData(dataBundle);

        currentState = newState;

        try {
            messenger.send(msg);
        } catch (Exception e) {}

        updateForegroundNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            int newState = currentState;
            if (!disconnectionStarted && this.currentState != VPNStates.ERROR && this.currentState != VPNStates.BLOCKING_ERROR) {
                newState = VPNStates.DISCONNECTING;
            }
            updateState(newState);

            stopRequested = true;
            disconnect();
        } else if (intent != null && ACTION_CONNECT.equals(intent.getAction())) {
            if (vpnInterface == null) {
                vpnInterface = new VPNWorkInterface(this, HelperFunctions.getOpenAppPendingIntent());

                if (VPNPersistentData.getProtectBeforeConnected()) {
                    try {
                        vpnInterface.configure(VPNWorkInterface.Modes.BLOCKING);
                    } catch (Exception e) { }
                }
            }

            if (intent.hasExtra(MESSENGER_PARAM)) {
                messenger = intent.getParcelableExtra(MESSENGER_PARAM);
                updateState(currentState);
            }

            checkInternetConnectionIfNeeded();

            VPNCoordinator.getInstance().informServiceRunning();
        } else if (intent != null) {
            HelperFunctions.showToast(getString(R.string.general_unable_to_start_error), false);

            PendingIntent openVpnSettingsPendingIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent openVpnSettingsIntent = new Intent(android.provider.Settings.ACTION_VPN_SETTINGS);
                openVpnSettingsPendingIntent = PendingIntent.getActivity(App.getContext(), 0, openVpnSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                openVpnSettingsPendingIntent = HelperFunctions.getOpenAppPendingIntent();
            }

            HelperFunctions.showAlertNotification(
                Globals.SYSTEM_START_ALERT_NOTIFICATION_ID,
                getString(R.string.general_app_name),
                getString(R.string.general_unavailable_system_feature_error),
                openVpnSettingsPendingIntent
            );
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Skywiremob.printString("VPN service destroyed.");
        stopRequested = true;
        disconnect();
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Skywiremob.printString("onRevoke called");
        this.stopSelf();
    }

    private void checkInternetConnectionIfNeeded() {
        // Become a foreground service. Background services can be VPN services too, but they can
        // be killed by background check before getting a chance to receive onRevoke().
        makeForeground();

        if (currentState != VPNStates.WAITING_FOR_CONNECTIVITY) {
            Skywiremob.printString("CHECKING CONNECTION");
            updateState(VPNStates.CHECKING_CONNECTIVITY);
        }

        if (checkConnectionSubscription != null) {
            checkConnectionSubscription.dispose();
        }

        if (visor == null) {
            checkConnectionSubscription = HelperFunctions.checkInternetConnectivity()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hasInternetConnection -> {
                    checkConnectionSubscription.dispose();

                    if (hasInternetConnection) {
                        startVisorIfNeeded();
                    } else {
                        updateState(VPNStates.WAITING_FOR_CONNECTIVITY);

                        checkConnectionSubscription = Observable.just(0).delay(1000, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(val -> {
                                checkInternetConnectionIfNeeded();
                            });
                    }
                });
        }
    }

    private void startVisorIfNeeded() {
        if (visor == null) {
            Skywiremob.printString("STARTING VISOR");

            visor = new VisorRunnable();

            visorSubscription = visor.run()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    updateState(state);

                    if (visorTimeoutSubscription != null) {
                        visorTimeoutSubscription.dispose();
                    }

                    visorTimeoutSubscription = Observable.just(0).delay(45000, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(val -> {
                            HelperFunctions.logError("VPN service", "Timeout preparing the visor.");
                            putInErrorState(this.getString(R.string.vpn_timerout_error));
                        });
                }, err -> {
                    putInErrorState(err.getLocalizedMessage());
                }, () -> {
                    visorTimeoutSubscription.dispose();
                    startConnection();
                });
        }
    }

    private void startConnection() {
        final SkywireVPNConnection connection = new SkywireVPNConnection(
            this,
            mNextConnectionId.getAndIncrement(),
            "localhost",
            7890,
            visor,
            vpnInterface
        );

        this.connectionRunnable = connection;

        vpnConnectionSubscription = connection.getObservable()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                val -> {
                    updateState(val);
                }, err -> {
                    putInErrorState(err.getLocalizedMessage());
                }, () -> {
                    // This event is not expected, but it would mean that the vpn connection is not longer active.
                    disconnect();
                }
            );
    }

    private void disconnect() {
        if (!disconnectionStarted) {
            disconnectionStarted = true;
            Skywiremob.printString("STOPPING ANDROID VPN SERVICE");

            if (this.currentState != VPNStates.ERROR && this.currentState != VPNStates.BLOCKING_ERROR) {
                updateState(VPNStates.DISCONNECTING);
            }

            if (checkConnectionSubscription != null) {
                checkConnectionSubscription.dispose();
            }
            if (visorSubscription != null) {
                visorSubscription.dispose();
            }
            if (visorTimeoutSubscription != null) {
                visorTimeoutSubscription.dispose();
            }
            if (vpnConnectionSubscription != null) {
                vpnConnectionSubscription.dispose();
            }
            if (this.connectionRunnable != null) {
                this.connectionRunnable.close();
            }

            Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                if (visor != null) {
                    visor.stopVisor();
                }
                emitter.onComplete();
            }).subscribeOn(Schedulers.newThread()).subscribe(val -> {});

            Observable.timer(100, TimeUnit.MILLISECONDS)
                .repeatUntil(new BooleanSupplier() {
                    @Override
                    public boolean getAsBoolean() {
                        if (!Skywiremob.isVisorStarting() && !Skywiremob.isVisorRunning()) {
                            if (disconnectionVerifications == 2) {
                                disconnectionFinished = true;
                                finishIfAppropiate();

                                return true;
                            } else {
                                disconnectionVerifications += 1;
                            }
                        } else {
                            if (disconnectionVerifications != 0) {
                                if (visor != null) {
                                    visor.stopVisor();
                                }
                            }

                            disconnectionVerifications = 0;
                        }

                        return false;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        } else {
            finishIfAppropiate();
        }
    }

    private void finishIfAppropiate() {
        if (disconnectionFinished && (stopRequested || !VPNPersistentData.getKillSwitchActivated())) {
            if (vpnInterface != null) {
                vpnInterface.close();

                // Create another interface and close it immediately to avoid a bug in older Android
                // versions when the app is added to the ignore list.
                vpnInterface = new VPNWorkInterface(this, HelperFunctions.getOpenAppPendingIntent());
                try {
                    vpnInterface.configure(VPNWorkInterface.Modes.DELETING);
                } catch (Exception e) { }
                vpnInterface.close();

                vpnInterface = null;
            }

            updateState(VPNStates.DISCONNECTED);
            stopForeground(true);
            stopSelf();
            notificationManager.cancel(Globals.SERVICE_STATUS_NOTIFICATION_ID);

            if (!HelperFunctions.appIsOnForeground() && !VPNPersistentData.getKillSwitchActivated() && VPNPersistentData.getLastError(null) != null) {
                HelperFunctions.showAlertNotification(
                    Globals.ERROR_NOTIFICATION_ID,
                    getString(R.string.general_app_name),
                    getString(R.string.general_connection_error),
                    HelperFunctions.getOpenAppPendingIntent()
                );
            }
        }
    }

    private void putInErrorState(String errorMsg) {
        lastErrorMsg = errorMsg;
        if (!vpnInterface.alreadyConfigured() || !VPNPersistentData.getKillSwitchActivated()) {
            stopRequested = true;
            updateState(VPNStates.ERROR);
        } else {
            updateState(VPNStates.BLOCKING_ERROR);
        }
        disconnect();
    }

    private void updateForegroundNotification() {
        notificationManager.notify(Globals.SERVICE_STATUS_NOTIFICATION_ID, createUpdatedNotification());
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
            } else if (alreadyConnected) {
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
