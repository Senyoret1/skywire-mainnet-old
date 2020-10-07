package com.skywire.skycoin.vpn.vpn;

import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import skywiremob.Skywiremob;

/**
 * Allows to easily control the starting and stopping procedures of the the visor and VPN client
 * included in Skywiremob.
 */
public class VisorRunnable {
    /**
     * If Skywiremob.prepareVPNClient has already been called without errors.
     */
    private boolean vpnClientStarted = false;
    /**
     * If Skywiremob.startListeningUDP() has already been called without errors.
     */
    private boolean listeningUdp = false;

    /**
     * Starts stopping the visor. It returns before the visor has been completely stopped.
     */
    public void startStoppingVisor() {
        String err = Skywiremob.stopVisor();
        if (!err.isEmpty()) {
            Skywiremob.printString(err);
            HelperFunctions.showToast(err, false);
        }
        Skywiremob.printString("Visor stopped");
    }

    /**
     * Stops the VPN client without stopping the visor.
     */
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

    /**
     * Starts the Skywire visor.
     * @return Observable that will emit the current state of the process, as variables defined in
     * VPNStates, and will complete after starting the visor.
     */
    public Observable<VPNStates> runVisor() {
        return Observable.create((ObservableOnSubscribe<VPNStates>) emitter -> {
            if (emitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.PREPARING_VISOR);

            // Start the visor if the emitter is still valid.
            if (emitter.isDisposed()) { return; }
            String err = Skywiremob.prepareVisor();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            // Block the thread while the visor is starting.
            err = Skywiremob.waitVisorReady();
            if (!err.isEmpty()) {
                HelperFunctions.logError("Visor startup procedure", err);
                if (emitter.isDisposed()) { return; }
                emitter.onError(new Exception(err));
                return;
            }

            // Finish.
            Skywiremob.printString("Prepared visor");
            if (emitter.isDisposed()) { return; }
            emitter.onNext(VPNStates.VISOR_READY);
            emitter.onComplete();
        });
    }

    /**
     * Starts the VPN client. This function was made to be used inside an observable which emits
     * the state of the VPN service.
     * @param parentEmitter Emitter of the observable from which this function was called, to be
     *                      able to emit the state changes.
     */
    public void runVpnClient(ObservableEmitter<VPNStates> parentEmitter) throws Exception {
        // Update the state.
        if (parentEmitter.isDisposed()) { return; }
        parentEmitter.onNext(VPNStates.PREPARING_VPN_CLIENT);

        // Prepare the VPN client with the last saved public key and password.
        if (parentEmitter.isDisposed()) { return; }
        String err = Skywiremob.prepareVPNClient(
            VPNPersistentData.getPublicKey(""),
            VPNPersistentData.getPassword("")
        );
        if (!err.isEmpty()) {
            throw new Exception(err);
        }
        vpnClientStarted = true;
        Skywiremob.printString("Prepared VPN client");
        if (parentEmitter.isDisposed()) { return; }
        parentEmitter.onNext(VPNStates.FINAL_PREPARATIONS_FOR_VISOR);

        // Perform the handshake.
        if (parentEmitter.isDisposed()) { return; }
        err = Skywiremob.shakeHands();
        if (!err.isEmpty()) {
            throw new Exception(err);
        }

        // Start listening.
        if (parentEmitter.isDisposed()) { return; }
        err = Skywiremob.startListeningUDP();
        listeningUdp = true;
        if (!err.isEmpty()) {
            throw new Exception(err);
        }

        // Start serving.
        if (parentEmitter.isDisposed()) { return; }
        err = Skywiremob.serveVPN();
        if (!err.isEmpty()) {
            throw new Exception(err);
        }
    }
}