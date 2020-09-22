package com.skywire.skycoin.vpn.vpn;

import com.skywire.skycoin.vpn.HelperFunctions;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import skywiremob.Skywiremob;

public class VisorRunnable {
    private boolean vpnClientStarted = false;
    private boolean listeningUdp = false;

    public void stopVisor() {
        String err = Skywiremob.stopVisor();
        if (!err.isEmpty()) {
            Skywiremob.printString(err);
            showToast(err);
        }
        Skywiremob.printString("Visor stopped");
    }

    public void stopVpnConnection() {
        if (vpnClientStarted) {
            Skywiremob.stopVPNClient();
            vpnClientStarted = false;
        }
        if (listeningUdp) {
            Skywiremob.stopListeningUDP();
            listeningUdp = false;
        }
        Skywiremob.printString("VPN connection stopped");
    }

    private void showToast(String text) {
        HelperFunctions.showToast(text, false);
    }

    public Observable<Integer> run() {
        return Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            if (emitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.PREPARING_VISOR);

            if (emitter.isDisposed()) { return; }
            String err = Skywiremob.prepareVisor();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            err = Skywiremob.waitVisorReady();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            Skywiremob.printString("Prepared visor");

            if (emitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.VISOR_READY);
            emitter.onComplete();
        });
    }

    public Observable<Integer> runVpnClient(ObservableEmitter<Integer> parentEmitter) {
        return Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.PREPARING_VPN_CLIENT);

            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            String err = Skywiremob.prepareVPNClient(
                VPNPersistentData.getPublicKey(""),
                VPNPersistentData.getPassword("")
            );
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }
            vpnClientStarted = true;
            Skywiremob.printString("Prepared VPN client");
            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.FINAL_PREPARATIONS_FOR_VISOR);

            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            err = Skywiremob.shakeHands();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            err = Skywiremob.startListeningUDP();
            listeningUdp = true;
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            err = Skywiremob.serveVPN();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            if (emitter.isDisposed() || parentEmitter.isDisposed()) { return; }
            emitter.onComplete();
        });
    }
}