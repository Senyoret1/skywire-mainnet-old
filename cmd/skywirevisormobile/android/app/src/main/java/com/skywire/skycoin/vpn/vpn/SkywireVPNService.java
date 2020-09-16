package com.skywire.skycoin.vpn.vpn;;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
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
    private PendingIntent mConfigureIntent;

    private int currentState = VPNStates.STARTING;
    private String lastErrorMsg;

    private VisorRunnable visor;
    private Disposable visorSubscription;
    private Disposable visorTimeoutSubscription;
    private Disposable vpnConnectionSubscription;

    private boolean startedByTheSystem = false;
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

        mConfigureIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void updateState(int newState) {
        Message msg = Message.obtain();
        msg.what = newState;

        Bundle dataBundle = new Bundle();
        dataBundle.putBoolean(STARTED_BY_THE_SYSTEM_PARAM, startedByTheSystem);
        dataBundle.putBoolean(STOP_REQUESTED_PARAM, stopRequested);

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
            if (intent.hasExtra(MESSENGER_PARAM)) {
                messenger = intent.getParcelableExtra(MESSENGER_PARAM);
                updateState(currentState);
            }

            if (vpnInterface == null) {
                vpnInterface = new VPNWorkInterface(this, mConfigureIntent);
            }
            startVisorIfNeeded();
        } else if (intent != null) {
            startedByTheSystem = true;
            updateState(currentState);

            if (!disconnectionStarted) {
                startVisorIfNeeded();
            } else {
                // Done as precaution.
                HelperFunctions.showToast(getString(R.string.tmp_general_service_stopping_error), false);
            }
        }

        VPNCoordinator.getInstance().informServiceRunning();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Skywiremob.printString("Closing service");
        disconnect();
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        Skywiremob.printString("onRevoke called");
        this.stopSelf();
    }

    private void startVisorIfNeeded() {
        // Become a foreground service. Background services can be VPN services too, but they can
        // be killed by background check before getting a chance to receive onRevoke().
        makeForeground();

        if (visor == null) {
            Skywiremob.printString("STARTING ANDROID VPN SERVICE");

            // Check if the device has network connectivity.
            boolean validNetwork = HelperFunctions.checkIfNetworkAvailable();
            if (!validNetwork) {
                HelperFunctions.logError("VPN service", "Trying to start the VPN service without network connection.");
                putInErrorState(this.getString(R.string.vpn_service_no_network_error));
                return;
            }

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
        if (disconnectionFinished && stopRequested) {
            if (vpnInterface != null) {
                vpnInterface.close();
            }

            updateState(VPNStates.DISCONNECTED);
            stopForeground(true);
            stopSelf();
            notificationManager.cancel(1);
        }
    }

    private void putInErrorState(String errorMsg) {
        lastErrorMsg = errorMsg;
        if (!vpnInterface.alreadyConfigured()) {
            stopRequested = true;
            updateState(VPNStates.ERROR);
        } else {
            updateState(VPNStates.BLOCKING_ERROR);
        }
        disconnect();
    }

    private void updateForegroundNotification() {
        notificationManager.notify(1, createUpdatedNotification());
    }

    private void makeForeground() {
        startForeground(1, createUpdatedNotification());
    }

    private Notification createUpdatedNotification() {
        return new NotificationCompat.Builder(this, Globals.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentTitle(getString(R.string.general_app_name))
            .setContentText(getString(getTextForState(currentState)))
            .setContentIntent(mConfigureIntent)
            .setOnlyAlertOnce(true)
            .build();
    }
}
