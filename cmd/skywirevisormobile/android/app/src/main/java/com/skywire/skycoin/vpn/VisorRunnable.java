package com.skywire.skycoin.vpn;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.skywire.skycoin.vpn.helpers.App;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import skywiremob.Skywiremob;

public class VisorRunnable {
    public void stopVisor() {
        String err = Skywiremob.stopVisor();
        if (!err.isEmpty()) {
            Skywiremob.printString(err);
            showToast(err);
        }
        Skywiremob.printString("Visor stopped");
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
            emitter.onNext(VPNStates.PREPARING_VPN_CLIENT);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());

            if (emitter.isDisposed()) { return; }
            err = Skywiremob.prepareVPNClient(
                VPNPersistentData.getPublicKey(""),
                VPNPersistentData.getPassword("")
            );
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }
            Skywiremob.printString("Prepared VPN client");
            if (emitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.FINAL_PREPARATIONS_FOR_VISOR);

            if (emitter.isDisposed()) { return; }
            err = Skywiremob.shakeHands();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            if (emitter.isDisposed()) { return; }
            err = Skywiremob.startListeningUDP();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            if (emitter.isDisposed()) { return; }
            err = Skywiremob.serveVPN();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            if (emitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.VISOR_READY);
            emitter.onComplete();
        });
    }
}