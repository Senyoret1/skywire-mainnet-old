package com.skywire.skycoin.vpn;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
            emitter.onNext(SkywireVPNService.States.PREPARING_VISOR);

            String err = Skywiremob.prepareVisor();
            if (!err.isEmpty()) {
                Skywiremob.printString(err);
                showToast(err);
                return;
            }
            Skywiremob.printString("Prepared visor");
            emitter.onNext(SkywireVPNService.States.PREPARING_VPN_CLIENT);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());

            err = Skywiremob.prepareVPNClient(
                settings.getString(Globals.StorageVars.SERVER_PK, ""),
                settings.getString(Globals.StorageVars.SERVER_PASSWORD, "")
            );
            if (!err.isEmpty()) {
                Skywiremob.printString(err);
                showToast(err);
                return;
            }
            Skywiremob.printString("Prepared VPN client");
            emitter.onNext(SkywireVPNService.States.FINAL_PREPARATIONS_FOR_VISOR);

            err = Skywiremob.shakeHands();
            if (!err.isEmpty()) {
                Skywiremob.printString(err);
                showToast(err);
                return;
            }

            err = Skywiremob.startListeningUDP();
            if (!err.isEmpty()) {
                Skywiremob.printString(err);
                showToast(err);
                return;
            }

            Skywiremob.serveVPN();

            emitter.onNext(SkywireVPNService.States.VISOR_READY);

            err = Skywiremob.waitForVisorToStop();
            if (!err.isEmpty()) {
                Skywiremob.printString(err);
                showToast(err);
                return;
            }
            Skywiremob.printString("Wait finished");

            emitter.onComplete();
        });
    }
}