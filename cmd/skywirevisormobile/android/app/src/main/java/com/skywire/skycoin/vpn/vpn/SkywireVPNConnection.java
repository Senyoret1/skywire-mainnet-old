package com.skywire.skycoin.vpn.vpn;

import android.net.VpnService;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.HelperFunctions;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import skywiremob.Skywiremob;

public class SkywireVPNConnection implements Closeable {
    private final VpnService service;
    private final int connectionId;
    private final String serverName;
    private final int serverPort;
    private final VisorRunnable visorRunnable;

    private VPNWorkInterface vpnInterface = null;
    private DatagramChannel tunnel = null;

    private String lastError = null;
    private Throwable operationError = null;
    private Throwable visorInitializationError = null;

    private Observable<Integer> observable;

    private Disposable sendingProcedureSubscription;
    private Disposable receivingProcedureSubscription;

    public SkywireVPNConnection(
        VpnService service,
        int connectionId,
        String serverName,
        int serverPort,
        VisorRunnable visorRunnable,
        VPNWorkInterface vpnInterface
    ) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverName = serverName;
        this.serverPort= serverPort;
        this.visorRunnable = visorRunnable;
        this.vpnInterface = vpnInterface;
    }

    @Override
    public void close() {
        closeConnection();
    }

    public Observable<Integer> getObservable() {
        if (observable == null) {
            observable = Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                try {
                    Skywiremob.printString(getTag() + " Starting");
                    // If anything needs to be obtained using the network, get it now.
                    // This greatly reduces the complexity of seamless handover, which
                    // tries to recreate the tunnel without shutting down everything.
                    // In this demo, all we need to know is the server address.
                    final SocketAddress serverAddress = new InetSocketAddress(serverName, serverPort);

                    if (VPNPersistentData.getMustRestartVpn()) {
                        // The code will restart the connection in case of problem, but only if the connection
                        // was established during the last attempt.
                        while (true) {
                            if (emitter.isDisposed()) {
                                return;
                            }

                            lastError = null;

                            if (!run(serverAddress, emitter)) {
                                break;
                            }

                            emitter.onNext(VPNStates.RESTORING_VPN);

                            if (emitter.isDisposed()) {
                                return;
                            }
                            Thread.sleep(2000);
                        }
                    } else {
                        run(serverAddress, emitter);
                    }

                    // Use this msg again if the code for retying the connection is reactivated.
                    // Skywiremob.printString(getTag() + " Maximum number of retries reached");

                    if (lastError == null) {
                        HelperFunctions.logError(getTag(), "The connection has been closed unexpectedly.");
                        if (emitter.isDisposed()) { return; }
                        emitter.onError(new Exception(App.getContext().getString(R.string.vpn_connection_finished_error)));
                    } else {
                        if (emitter.isDisposed()) { return; }
                        emitter.onError(new Exception(lastError));
                    }
                } catch (Exception e) {
                    HelperFunctions.logError(getTag() + " Connection failed, exiting", e);
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }

                emitter.onComplete();
            });
        }

        return observable;
    }

    private boolean run(SocketAddress server, ObservableEmitter<Integer> parentEmitter) {
        boolean connected = false;

        lastError = null;
        operationError = null;

        // TODO: delete if the code for protecting the sockets is removed.
        // String protectErrorMsg = App.getContext().getString(R.string.vpn_socket_protection_error);

        // Create a DatagramChannel as the VPN tunnel.
        try {
            visorInitializationError = null;
            visorRunnable.runVpnClient(parentEmitter).subscribe(val -> {
                parentEmitter.onNext(val);
            }, err -> {
                visorInitializationError = err;
            });

            if (visorInitializationError != null) {
                throw visorInitializationError;
            }

            if (parentEmitter.isDisposed()) { return connected; }
            tunnel = DatagramChannel.open();

            if (parentEmitter.isDisposed()) { return connected; }

            // TODO: this code is used for protecting the sockets (make them bypass vpn protection)
            // needed for configuration, to avoid infinite loops. This is not currently needed
            // because there is an exception that covers the entire application. The code remains
            // here as a precaution and should be removed in the future.
            /*
            // Protect the tunnel before connecting to avoid loopback.
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

            // Connect to the server.
            if (parentEmitter.isDisposed()) { return connected; }
            tunnel.connect(server);

            // Inform Skywire about the local socket address.
            // NOTE: this function should work in old Android versions, but there is a bug, at least in
            // Android API 17, which makes the port to always be 0, that is why the app requires Android
            // API 21+ to run. Maybe creating the socket by hand would allow to support older versions.
            if (parentEmitter.isDisposed()) { return connected; }
            Skywiremob.setMobileAppAddr(tunnel.socket().getLocalSocketAddress().toString());

            tunnel.configureBlocking(true);
            // Configure the virtual network interface. This starts the VPN protection in the OS.
            if (parentEmitter.isDisposed()) { return connected; }
            vpnInterface.configure();
            // Now we are connected. Set the flag.
            connected = true;
            parentEmitter.onNext(VPNStates.CONNECTED);

            // We keep forwarding packets till something goes wrong.
            Skywiremob.printString(getTag() + " is forwarding packets on Android");

            sendingProcedureSubscription = VPNDataManager.createObservable(vpnInterface, tunnel, true)
                .subscribeOn(Schedulers.newThread()).subscribe(
                    val -> {},
                    err -> {
                        synchronized (service) {
                            if (operationError == null) {
                                operationError = err;
                            }
                        }
                    }
                );
            receivingProcedureSubscription = VPNDataManager.createObservable(vpnInterface, tunnel, false)
                .subscribeOn(Schedulers.newThread()).subscribe(
                    val -> {},
                    err -> {
                        synchronized (service) {
                            if (operationError == null) {
                                operationError = err;
                            }
                        }
                    }
                );

            while (true) {
                if (parentEmitter.isDisposed()) {
                    break;
                }

                synchronized (service) {
                    if (operationError != null) {
                        throw operationError;
                    }
                }

                if (sendingProcedureSubscription.isDisposed() || receivingProcedureSubscription.isDisposed()) {
                    break;
                }

                Thread.sleep(2000);
            }
        } catch (Throwable e) {
            if (!parentEmitter.isDisposed()) {
                HelperFunctions.logError(getTag() + " Cannot use socket", e);
                lastError = e.getLocalizedMessage();
            }
        } finally {
            closeConnection();
        }

        return connected;
    }

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
                HelperFunctions.logError(getTag() + " Unable to close tunnel", e);
            }
        }
    }

    private final String getTag() {
        return SkywireVPNConnection.class.getSimpleName() + "[" + connectionId + "]";
    }
}
