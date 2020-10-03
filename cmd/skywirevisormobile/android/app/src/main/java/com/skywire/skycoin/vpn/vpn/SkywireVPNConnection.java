package com.skywire.skycoin.vpn.vpn;

import android.net.VpnService;

import com.skywire.skycoin.vpn.Globals;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.HelperFunctions;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import skywiremob.Skywiremob;

/**
 * Class in charge of finishing starting the visor and connect it with the VPN work interface,
 * to make the VPN functional.
 */
public class SkywireVPNConnection implements Closeable {
    /**
     * Current VPN service instance.
     */
    private final VpnService service;
    /**
     * Object for controlling the local visor.
     */
    private final VisorRunnable visorRunnable;
    /**
     * Current VPN work interface.
     */
    private VPNWorkInterface vpnInterface = null;
    /**
     * Tunnel for communicating with the local visor.
     */
    private DatagramChannel tunnel = null;

    /**
     * Error message returned during the last call to the function for making the VPN connection
     * work, if any.
     */
    private String lastError = null;
    /**
     * Last error returned by a procedure for sending or receiving data in another thread, if any.
     */
    private Throwable operationError = null;
    /**
     * Observable used by this instance to make the VPN connection work.
     */
    private Observable<Integer> observable;

    private Disposable sendingProcedureSubscription;
    private Disposable receivingProcedureSubscription;

    public SkywireVPNConnection(
        VpnService service,
        VisorRunnable visorRunnable,
        VPNWorkInterface vpnInterface
    ) {
        this.service = service;
        this.visorRunnable = visorRunnable;
        this.vpnInterface = vpnInterface;
    }

    /**
     * Stops all operations and frees the resources used by this instance.
     */
    @Override
    public void close() {
        closeConnection();
    }

    /**
     * Creates an observable with the procedure for finishing the visor initialization and
     * connecting the VPN interface with it, which makes the whole VPN protection start working.
     * @return Observable which emits the current state, using the constants defined in VPNStates.
     * The observable is not expected to complete, just emit and return errors.
     */
    public Observable<Integer> getObservable() {
        // A new observable is created only if needed.
        if (observable == null) {
            observable = Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                try {
                    Skywiremob.printString("Starting VPN connection");

                    if (VPNPersistentData.getMustRestartVpn()) {
                        // The code will restart the connection in case of problem, but only if
                        // the connection was established during the last attempt.
                        while (true) {
                            // Stop if the emitter is no longer valid.
                            if (emitter.isDisposed()) { return; }

                            lastError = null;

                            // Break if the attempt was not able to finish the connection.
                            if (!run(emitter)) {
                                break;
                            }

                            // Retry after a small delay.
                            emitter.onNext(VPNStates.RESTORING_VPN);
                            if (emitter.isDisposed()) {
                                return;
                            }
                            Thread.sleep(2000);
                        }
                    } else {
                        // Try to make the connection one time only.
                        run(emitter);
                    }

                    // Finish with an error.
                    if (lastError == null) {
                        HelperFunctions.logError("VPN connection", "The connection has been closed unexpectedly.");
                        if (emitter.isDisposed()) { return; }
                        emitter.onError(new Exception(App.getContext().getString(R.string.vpn_connection_finished_error)));
                    } else {
                        HelperFunctions.logError("VPN connection", lastError);
                        if (emitter.isDisposed()) { return; }
                        emitter.onError(new Exception(lastError));
                    }
                } catch (Exception e) {
                    HelperFunctions.logError("The VPN connection failed, exiting", e);
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }

                // This should never happen, as an error should have been reported before.
                if (emitter.isDisposed()) { return; }
                emitter.onComplete();
            });
        }

        return observable;
    }

    /**
     * Finish the visor initialization and connects the VPN interface with it, establishing the
     * VPN connection. It is expected to run indefinitely and return only in case of error.
     * @return True if the connections was established before the function finished.
     */
    private boolean run(ObservableEmitter<Integer> parentEmitter) {
        boolean connected = false;

        // Reset the error vars, to indicate that no errors have occurred during this execution of
        // the function.
        lastError = null;
        operationError = null;

        // TODO: delete if the code for protecting the sockets is removed.
        // String protectErrorMsg = App.getContext().getString(R.string.vpn_socket_protection_error);

        try {
            // Finish the visor initialization.
            visorRunnable.runVpnClient(parentEmitter);

            // Create a DatagramChannel for connecting with the local visor.
            if (parentEmitter.isDisposed()) { return connected; }
            tunnel = DatagramChannel.open();

            // TODO: this code is used for protecting the sockets (make them bypass vpn protection)
            // needed for configuration, to avoid infinite loops. This is not currently needed
            // because there is an exception that covers the entire application. The code remains
            // here as a precaution and should be removed in the future.
            /*
            // Protect the tunnel before connecting to avoid loopback.
            if (parentEmitter.isDisposed()) { return connected; }
            if (!service.protect(tunnel.socket())) {
                HelperFunctions.logError(getTag(), "Cannot protect the app-visor socket");
                throw new IllegalStateException(protectErrorMsg);
            }
            while(true) {
                if (parentEmitter.isDisposed()) { return connected; }

                int fd = (int) Skywiremob.nextDmsgSocket();
                if (fd == 0) { break; }

                Skywiremob.printString("PRINTING FD " + fd);
                if (!service.protect(fd)) {
                    HelperFunctions.logError(getTag(), "Cannot protect the socket for " + fd);
                    throw new IllegalStateException(protectErrorMsg);
                }
            }
            */

            // Connect to the local visor.
            if (parentEmitter.isDisposed()) { return connected; }
            tunnel.connect(new InetSocketAddress(Globals.LOCAL_VISOR_ADDRESS, Globals.LOCAL_VISOR_PORT));

            // Inform the local socket address to Skywiremob.
            // NOTE: this function should work in old Android versions, but there is a bug, at
            // least in Android API 17, which makes the port to always be 0, that is why the app
            // requires Android API 21+ to run. Maybe creating the socket by hand would allow to
            // upport older versions.
            if (parentEmitter.isDisposed()) { return connected; }
            Skywiremob.setMobileAppAddr(tunnel.socket().getLocalSocketAddress().toString());

            // Make the data operations synchronous.
            tunnel.configureBlocking(true);
            // Configure the virtual network interface. This activates the VPN protection in the
            // OS, if it is being done for the first time.
            if (parentEmitter.isDisposed()) { return connected; }
            vpnInterface.configure(VPNWorkInterface.Modes.WORKING);
            // Inform the connection.
            if (parentEmitter.isDisposed()) { return connected; }
            connected = true;
            parentEmitter.onNext(VPNStates.CONNECTED);

            Skywiremob.printString("The VPN connection is forwarding packets on Android");

            // Create an observable for sending data in another thread.
            sendingProcedureSubscription = VPNDataManager.createObservable(vpnInterface, tunnel, true)
                .subscribeOn(Schedulers.newThread()).subscribe(
                    val -> {},
                    err -> {
                        synchronized (service) {
                            // Save the error, to use it below.
                            if (operationError == null) {
                                operationError = err;
                            }
                        }
                    }
                );
            // Create an observable for receiving data in another thread.
            receivingProcedureSubscription = VPNDataManager.createObservable(vpnInterface, tunnel, false)
                .subscribeOn(Schedulers.newThread()).subscribe(
                    val -> {},
                    err -> {
                        synchronized (service) {
                            // Save the error, to use it below.
                            if (operationError == null) {
                                operationError = err;
                            }
                        }
                    }
                );

            // Wait until the emitter stops being valid or any of the previous procedures fail.
            while (true) {
                if (parentEmitter.isDisposed()) {
                    break;
                }

                // If there was an error receiving or sending data, or any of those procedures
                // finished, end the loop.
                synchronized (service) {
                    if (operationError != null) {
                        throw operationError;
                    }
                }
                if (sendingProcedureSubscription.isDisposed() || receivingProcedureSubscription.isDisposed()) {
                    break;
                }

                // Wait before checking again.
                Thread.sleep(2000);
                connected = false;
                throw new Error("123");
            }
        } catch (Throwable e) {
            // Report the error.
            if (!parentEmitter.isDisposed()) {
                HelperFunctions.logError("VPN connector work procedure", e);
                lastError = e.getLocalizedMessage();
            }
        } finally {
            // CLose the connection.
            closeConnection();
        }

        return connected;
    }

    /**
     * Closes any open connection, stops the VPN client and stops the the pending threads.
     */
    private void closeConnection() {
        if (sendingProcedureSubscription != null) {
            sendingProcedureSubscription.dispose();
        }
        if (receivingProcedureSubscription != null) {
            receivingProcedureSubscription.dispose();
        }

        visorRunnable.stopVpnConnection();

        if (tunnel != null) {
            try {
                tunnel.close();
                tunnel = null;
            } catch (IOException e) {
                HelperFunctions.logError("Unable to close tunnel used by the VPN connection", e);
            }
        }
    }
}
