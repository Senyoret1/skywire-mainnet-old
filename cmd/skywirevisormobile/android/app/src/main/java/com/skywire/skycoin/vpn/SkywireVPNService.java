package com.skywire.skycoin.vpn;;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;

import com.skywire.skycoin.vpn.helpers.App;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import skywiremob.Skywiremob;

public class SkywireVPNService extends VpnService {
    public static class States {
        public static int STARTING = 10;
        public static int PREPARING_VISOR = 20;
        public static int PREPARING_VPN_CLIENT = 30;
        public static int FINAL_PREPARATIONS_FOR_VISOR = 35;
        public static int VISOR_READY = 40;
        public static int STARTING_VPN_CONNECTION = 50;
        public static int CONNECTED = 100;
        public static int RECONNECTING = 110;
        public static int DISCONNECTING = 200;
        public static int DISCONNECTED = 300;
        public static int ERROR = 400;
    }

    static int getTextForState(int state) {
        if (state == States.STARTING) {
            return R.string.vpn_state_initializing;
        } else if (state == States.PREPARING_VISOR) {
            return R.string.vpn_state_starting_visor;
        } else if (state == States.PREPARING_VPN_CLIENT) {
            return R.string.vpn_state_starting_vpn_app;
        } else if (state == States.FINAL_PREPARATIONS_FOR_VISOR) {
            return R.string.vpn_state_additional_visor_initializations;
        } else if (state == States.VISOR_READY) {
            return R.string.vpn_state_connecting;
        } else if (state == States.STARTING_VPN_CONNECTION) {
            return R.string.vpn_state_connecting;
        } else if (state == States.CONNECTED) {
            return R.string.vpn_state_connected;
        } else if (state == States.RECONNECTING) {
            return R.string.vpn_state_reconnecting;
        } else if (state == States.DISCONNECTING) {
            return R.string.vpn_state_disconnecting;
        } else if (state == States.DISCONNECTED) {
            return R.string.vpn_state_disconnected;
        } else if (state == States.ERROR) {
            return R.string.vpn_state_error;
        }

        return -1;
    }

    public static final String ACTION_CONNECT = "com.skywire.android.vpn.START";
    public static final String ACTION_DISCONNECT = "com.skywire.android.vpn.STOP";
    public static final String ACTION_STOP_COMUNNICATION = "com.skywire.android.vpn.STOP_COMM";

    public static final String ERROR_MSG_PARAM = "ErrorMsg";

    private HashMap<Integer, Messenger> messengers = new HashMap<>();

    private SkywireVPNConnection connectionRunnable;

    private final AtomicReference<ParcelFileDescriptor> mConnection = new AtomicReference<>();
    private AtomicInteger mNextConnectionId = new AtomicInteger(1);
    private PendingIntent mConfigureIntent;

    private int currentState = States.STARTING;
    private String lastErrorMsg;
    private boolean stopRequested;

    private VisorRunnable visor;
    private Disposable visorSubscription;
    private Disposable startVpnSubscription;
    private Disposable vpnConnectionSubscription;

    private boolean diconnected = false;

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
        currentState = newState;

        Message msg = Message.obtain();
        msg.what = currentState;

        if (newState == States.ERROR) {
            Bundle b = new Bundle();
            b.putString(ERROR_MSG_PARAM, lastErrorMsg);
            msg.setData(b);

            if (messengers.size() == 0) {
                HelperFunctions.showToast(lastErrorMsg, false);
            }
        }

        for(Map.Entry<Integer, Messenger> entry : messengers.entrySet()) {
            try {
                entry.getValue().send(msg);
            } catch (Exception e) {}
        }

        updateForegroundNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            if (!stopRequested) {
                stopRequested = true;
                Skywiremob.printString("STOPPING ANDROID VPN SERVICE");
                disconnect();
                updateState(States.DISCONNECTING);
            }
            return START_NOT_STICKY;
        } else if (intent != null && ACTION_CONNECT.equals(intent.getAction())) {
            // Become a foreground service. Background services can be VPN services too, but they can
            // be killed by background check before getting a chance to receive onRevoke().
            makeForeground();

            if (intent.hasExtra("ID") && intent.hasExtra("Messenger")) {
                messengers.put(intent.getIntExtra("ID", 0), intent.getParcelableExtra("Messenger"));
                updateState(currentState);
            }

            if (visor == null) {
                Skywiremob.printString("STARTING ANDROID VPN SERVICE");

                visor = new VisorRunnable();

                visorSubscription = visor.run()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(state -> {
                        updateState(state);

                        if (state == States.VISOR_READY) {
                            connect();
                        }
                    }, err -> {
                        lastErrorMsg = err.getLocalizedMessage();
                        updateState(States.ERROR);
                    }, () -> {
                        // TODO: after making the changes to stop and restart the visor, this may need changes.
                        if (currentState != States.ERROR) {
                            updateState(States.DISCONNECTED);
                        }
                    });
            }

            return START_STICKY;
        } else if (intent != null && ACTION_STOP_COMUNNICATION.equals(intent.getAction())) {
            if (intent.hasExtra("ID")) {
                messengers.remove(intent.getIntExtra("ID", 0));
            }

            return START_STICKY;
        } else {
            // TODO: manage the case.
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Skywiremob.printString("Closing service");
        disconnect();
    }

    private void connect() {
        updateState(States.STARTING_VPN_CONNECTION);

        startVpnSubscription = Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            try {
                if (emitter.isDisposed()) { return; }
                while (!Skywiremob.isVPNReady()) {
                    Skywiremob.printString("VPN STILL NOT READY, WAITING...");
                    Thread.sleep(1000);
                    if (emitter.isDisposed()) { return; }
                }

                emitter.onComplete();
            } catch (Exception e) {
                if (emitter.isDisposed()) { return; }
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(
            val -> {},
            err -> {
                lastErrorMsg = err.getLocalizedMessage();
                updateState(States.ERROR);
            }, () -> {
                Skywiremob.printString("VPN IS READY, LET'S TRY IT OUT");

                startConnection();
            }
        );
    }

    private void startConnection() {
        // Cancel any previous connection threads.
        if (vpnConnectionSubscription != null) {
            vpnConnectionSubscription.dispose();
        }
        if (this.connectionRunnable != null) {
            this.connectionRunnable.dispose();
        }

        final SkywireVPNConnection connection = new SkywireVPNConnection(
            this,
            mNextConnectionId.getAndIncrement(),
            "localhost",
            7890,
            mConfigureIntent
        );

        this.connectionRunnable = connection;

        vpnConnectionSubscription = connection.getObservable()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                val -> {
                    if (val) {
                        updateState(States.CONNECTED);
                    } else {
                        updateState(States.RECONNECTING);
                    }
                }, err -> {
                    lastErrorMsg = err.getLocalizedMessage();
                    updateState(States.ERROR);
                    disconnect();
                }, () -> {
                    // TODO: maybe this should not happen, so it would have to be managed to reflect that.
                    disconnect();
                }
            );
    }

    private void disconnect() {
        if (!diconnected) {
            if (this.currentState != States.ERROR) {
                updateState(States.DISCONNECTING);
            }

            // NOTE: if this is done, the subscription will not know when the visor has been stopped.
            //if (visorSubscription != null) {
            //    visorSubscription.dispose();
            //}

            if (vpnConnectionSubscription != null) {
                vpnConnectionSubscription.dispose();
            }
            if (startVpnSubscription != null) {
                startVpnSubscription.dispose();
            }
            stopForeground(true);

            //TODO: reactivate when making the changes to stop and restart the visor.
            //visor.stopVisor();

            diconnected = true;
        }
    }

    private void updateForegroundNotification() {
        NotificationManager mNotificationManager = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, createUpdatedNotification());
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
