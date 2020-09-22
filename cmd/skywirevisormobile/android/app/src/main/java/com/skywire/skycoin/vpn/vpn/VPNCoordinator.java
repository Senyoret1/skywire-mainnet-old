package com.skywire.skycoin.vpn.vpn;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.HelperFunctions;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import skywiremob.Skywiremob;

import static android.app.Activity.RESULT_OK;

public class VPNCoordinator implements Handler.Callback {
    public static final int VPN_PREPARATION_REQUEST_CODE = 1000100;

    private static VPNCoordinator instance = new VPNCoordinator();
    public static VPNCoordinator getInstance() {
        return instance;
    }

    private final Context ctx = App.getContext();

    private Handler serviceCommunicationHandler;
    private BehaviorSubject<VPNStates.StateInfo> eventsSubject = BehaviorSubject.create();

    private boolean serviceShouldBeRunning = false;

    private VPNCoordinator() {
        serviceCommunicationHandler = new Handler(this);

        eventsSubject.onNext(new VPNStates.StateInfo(VPNStates.OFF, false, null, false));

        if (isServiceRunning()) {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        VPNStates.StateInfo state = new VPNStates.StateInfo(
            msg.what,
            msg.getData().getBoolean(SkywireVPNService.STARTED_BY_THE_SYSTEM_PARAM),
            msg.getData().getString(SkywireVPNService.ERROR_MSG_PARAM),
            msg.getData().getBoolean(SkywireVPNService.STOP_REQUESTED_PARAM)
        );

        if (msg.what == VPNStates.DISCONNECTED) {
            serviceShouldBeRunning = false;
        }

        // Must be dore before informing about the event.
        if (msg.what == VPNStates.ERROR || msg.what == VPNStates.BLOCKING_ERROR) {
            VPNPersistentData.setLastError(msg.getData().getString(SkywireVPNService.ERROR_MSG_PARAM));
        }

        eventsSubject.onNext(state);

        return true;
    }

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) App.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SkywireVPNService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public Observable<VPNStates.StateInfo> getEventsObservable() {
        return eventsSubject.hide();
    }

    public void informServiceRunning() {
        if (!serviceShouldBeRunning && isServiceRunning()) {
            onActivityResult(VPN_PREPARATION_REQUEST_CODE, RESULT_OK, null);
        }
    }

    public void startVPN(Activity requestingActivity, String remotePK, String passcode) {
        // Check if the pk is valid.
        String err = Skywiremob.isPKValid(remotePK);
        if (!err.isEmpty()) {
            HelperFunctions.logError("Invalid PK starting service", err);
            HelperFunctions.showToast(ctx.getString(R.string.vpn_coordinator_invalid_credentials_error) + remotePK, false);
            return;
        } else {
            Skywiremob.printString("PK is correct");
        }

        VPNPersistentData.setPublicKeyAndPassword(remotePK, passcode);

        VPNPersistentData.removeLastError();
        eventsSubject.onNext(new VPNStates.StateInfo(VPNStates.STARTING, false, null, false));
        Intent intent = VpnService.prepare(requestingActivity);
        if (intent != null) {
            requestingActivity.startActivityForResult(intent, VPN_PREPARATION_REQUEST_CODE);
        } else {
            onActivityResult(VPN_PREPARATION_REQUEST_CODE, RESULT_OK, null);
        }
    }

    public void stopVPN() {
        ctx.startService(getServiceIntent(false).setAction(SkywireVPNService.ACTION_DISCONNECT));
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (request == VPN_PREPARATION_REQUEST_CODE) {
            if (result == RESULT_OK) {
                serviceShouldBeRunning = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(getServiceIntent(true).setAction(SkywireVPNService.ACTION_CONNECT));
                } else {
                    ctx.startService(getServiceIntent(true).setAction(SkywireVPNService.ACTION_CONNECT));
                }
            } else {
                eventsSubject.onNext(new VPNStates.StateInfo(VPNStates.OFF, false, null, true));
            }
        }
    }

    private Intent getServiceIntent(boolean IncludeExtras) {
        Intent response = new Intent(ctx, SkywireVPNService.class);
        if (IncludeExtras) {
            response.putExtra(SkywireVPNService.MESSENGER_PARAM, new Messenger(serviceCommunicationHandler));
        }
        return response;
    }
}
