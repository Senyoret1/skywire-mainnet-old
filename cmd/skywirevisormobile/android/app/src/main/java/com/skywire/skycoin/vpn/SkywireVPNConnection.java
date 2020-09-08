package com.skywire.skycoin.vpn;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.skywire.skycoin.vpn.helpers.HelperFunctions;

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

public class SkywireVPNConnection implements Disposable {
    private boolean disposed = false;

    private final VpnService service;
    private final int connectionId;
    private final String serverName;
    private final int serverPort;
    private final PendingIntent configureIntent;

    private ParcelFileDescriptor vpnInterface = null;
    private DatagramChannel tunnel = null;

    private String lastError = null;
    private Throwable operationError = null;

    private Observable<Boolean> observable;

    private Disposable sendingProcedureSubscription;
    private Disposable receivingProcedureSubscription;

    public SkywireVPNConnection(
        VpnService service,
        int connectionId,
        String serverName,
        int serverPort,
        PendingIntent configureIntent
    ) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverName = serverName;
        this.serverPort= serverPort;
        this.configureIntent = configureIntent;
    }

    @Override
    public void dispose() {
        disposed = true;
        closeConnection();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    public Observable<Boolean> getObservable() {
        if (observable == null) {
            observable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                try {
                    Skywiremob.printString(getTag() + " Starting");
                    // If anything needs to be obtained using the network, get it now.
                    // This greatly reduces the complexity of seamless handover, which
                    // tries to recreate the tunnel without shutting down everything.
                    // In this demo, all we need to know is the server address.
                    final SocketAddress serverAddress = new InetSocketAddress(serverName, serverPort);

                    // TODO: this code has ben deactivated because it is not really posible to start the conection again
                    // and inform the visor about the new socket as just after calling Skywiremob.setMobileAppAddr for the
                    //second time there is a panic.
                    /*
                    // We try to create the tunnel several times.
                    // Here we just use a counter to keep things simple.
                    for (int attempt = 0; attempt < 10; ++attempt) {
                        if (emitter.isDisposed()) { return; }
                        // Reset the counter if we were connected.
                        if (run(serverAddress, emitter)) {
                            attempt = 0;
                        }

                        // Sleep for a while. This also checks if we got interrupted.
                        if (emitter.isDisposed()) { return; }
                        Thread.sleep(3000);
                    }
                    */

                    run(serverAddress, emitter);

                    // Use this msg again if the code for retying the connection is reactivated.
                    // Skywiremob.printString(getTag() + " Maximum number of retries reached");

                    if (lastError == null) {
                        Skywiremob.printString(getTag() + " Connection failed.");
                        if (emitter.isDisposed()) { return; }
                        emitter.onError(new Exception("Connection failed."));
                    } else {
                        Skywiremob.printString(getTag() + " Connection failed. Last status message: " + lastError);
                        if (emitter.isDisposed()) { return; }
                        emitter.onError(new Exception("Connection failed. Last status message: " + lastError));
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

    private boolean run(SocketAddress server, ObservableEmitter<Boolean> parentEmitter) {
        boolean connected = false;

        lastError = null;
        operationError = null;

        String protectErrorMsg =  "The OS rejected the connection. Please make sure you have network connectivity and the application still have the permissions required. If you find no ptoblems, please restart the device.";

        // Create a DatagramChannel as the VPN tunnel.
        try {
            tunnel = DatagramChannel.open();

            if (parentEmitter.isDisposed()) { return connected; }
            // Protect the tunnel before connecting to avoid loopback.
            if (!service.protect(tunnel.socket())) {
                Skywiremob.printString("Cannot protect the app-visor socket");
                throw new IllegalStateException(protectErrorMsg);
            }

            while(true) {
                if (parentEmitter.isDisposed()) { return connected; }

                int fd = (int) Skywiremob.nextDmsgSocket();
                if (fd == 0) { break; }

                Skywiremob.printString("PRINTING FD " + fd);
                if (!service.protect(fd)) {
                    Skywiremob.printString("Cannot protect the socket for " + fd);
                    throw new IllegalStateException(protectErrorMsg);
                }
            }

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
            // Configure the virtual network interface.
            if (parentEmitter.isDisposed()) { return connected; }
            vpnInterface = configure();
            // Now we are connected. Set the flag.
            connected = true;
            parentEmitter.onNext(true);

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

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                vpnInterface = null;
            } catch (IOException e) {
                HelperFunctions.logError(getTag() + " Unable to close interface", e);
            }
        }

        if (tunnel != null) {
            try {
                tunnel.close();
                tunnel = null;
            } catch (IOException e) {
                HelperFunctions.logError(getTag() + " Unable to close tunnel", e);
            }
        }
    }

    private ParcelFileDescriptor configure() throws IllegalArgumentException {
        // Configure a builder while parsing the parameters.
        VpnService.Builder builder = service.new Builder();

        builder.setMtu((short)Skywiremob.getMTU());
        Skywiremob.printString("TUN IP: " + Skywiremob.tunip());
        builder.addAddress(Skywiremob.tunip(), (int)Skywiremob.getTUNIPPrefix());
        builder.addDnsServer("8.8.8.8");
        //builder.addDnsServer("192.168.1.1");
        builder.addRoute("0.0.0.0", 1);
        builder.addRoute("128.0.0.0", 1);
        builder.setBlocking(true);

        // Create a new interface using the builder and save the parameters.
        final ParcelFileDescriptor vpnInterface;

        builder.setConfigureIntent(configureIntent);
        synchronized (service) {
            vpnInterface = builder.establish();
        }
        Skywiremob.printString(getTag() + " New interface: " + vpnInterface);
        return vpnInterface;
    }

    private final String getTag() {
        return SkywireVPNConnection.class.getSimpleName() + "[" + connectionId + "]";
    }
}
