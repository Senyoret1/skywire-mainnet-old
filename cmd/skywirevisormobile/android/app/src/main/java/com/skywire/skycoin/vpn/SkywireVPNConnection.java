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

public class SkywireVPNConnection {
    /**
     * Callback interface to let the {@link SkywireVPNService} know about new connections
     * and update the foreground notification with connection status.
     */
    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    private final VpnService mService;
    private final int mConnectionId;
    private final String mServerName;
    private final int mServerPort;
    private PendingIntent mConfigureIntent;
    private OnEstablishListener mOnEstablishListener;

    private Observable<Boolean> observable;

    private Disposable sendingProcedureSubscription;
    private Disposable receivingProcedureSubscription;

    public SkywireVPNConnection(final VpnService service, final int connectionId, final String serverName, final int serverPort) {
        mService = service;
        mConnectionId = connectionId;
        mServerName = serverName;
        mServerPort= serverPort;
    }

    /**
     * Optionally, set an intent to configure the VPN. This is {@code null} by default.
     */
    public void setConfigureIntent(PendingIntent intent) {
        mConfigureIntent = intent;
    }

    public void setOnEstablishListener(OnEstablishListener listener) {
        mOnEstablishListener = listener;
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
                    final SocketAddress serverAddress = new InetSocketAddress(mServerName, mServerPort);
                    // We try to create the tunnel several times.
                    // TODO: The better way is to work with ConnectivityManager, trying only when the
                    // network is available.
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
                    Skywiremob.printString(getTag() + " Giving");

                    if (emitter.isDisposed()) { return; }
                    emitter.onError(new Exception("Giving"));
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
        ParcelFileDescriptor iface = null;
        boolean connected = false;

        // Create a DatagramChannel as the VPN tunnel.
        try (DatagramChannel tunnel = DatagramChannel.open()) {
            if (parentEmitter.isDisposed()) { return connected; }
            // Protect the tunnel before connecting to avoid loopback.
            if (!mService.protect(tunnel.socket())) {
                throw new IllegalStateException("Cannot protect the tunnel");
            }

            // TODO: use something better for detecting the state of the subscription.
            for (int fd = (int) Skywiremob.nextDmsgSocket(); fd != 0; fd = (int) Skywiremob.nextDmsgSocket()) {
                Skywiremob.printString("PRINTING FD " + fd);
                if (!mService.protect(fd)) {
                    throw new IllegalStateException("Cannot protect the tunnel");
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

            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            // TODO: now there are 2 threads.
            tunnel.configureBlocking(false);
            // Configure the virtual network interface.
            if (parentEmitter.isDisposed()) { return connected; }
            iface = configure();
            // Now we are connected. Set the flag.
            connected = true;

            // We keep forwarding packets till something goes wrong.
            Skywiremob.printString(getTag() + " is forwarding packets on Android");

            sendingProcedureSubscription = VPNDataManager.createObservable(iface, tunnel, true)
                .subscribeOn(Schedulers.io()).subscribe(
                    val -> {},
                    err -> { throw err; }
                );
            receivingProcedureSubscription = VPNDataManager.createObservable(iface, tunnel, false)
                .subscribeOn(Schedulers.io()).subscribe(
                    val -> {},
                    err -> { throw err; }
                );

            while (!sendingProcedureSubscription.isDisposed() && !receivingProcedureSubscription.isDisposed()) {
                if (parentEmitter.isDisposed()) { break; }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            HelperFunctions.logError(getTag() + " Cannot use socket", e);
        } finally {
            if (iface != null) {
                try {
                    iface.close();
                } catch (IOException e) {
                    HelperFunctions.logError(getTag() + " Unable to close interface", e);
                }
            }

            if (sendingProcedureSubscription != null) {
                sendingProcedureSubscription.dispose();
            }
            if (receivingProcedureSubscription != null) {
                receivingProcedureSubscription.dispose();
            }
        }

        return connected;
    }

    private ParcelFileDescriptor configure() throws IllegalArgumentException {
        // Configure a builder while parsing the parameters.
        VpnService.Builder builder = mService.new Builder();

        builder.setMtu((short)Skywiremob.getMTU());
        Skywiremob.printString("TUN IP: " + Skywiremob.tunip());
        builder.addAddress(Skywiremob.tunip(), (int)Skywiremob.getTUNIPPrefix());
        builder.addDnsServer("8.8.8.8");
        //builder.addDnsServer("192.168.1.1");
        builder.addRoute("0.0.0.0", 1);
        builder.addRoute("128.0.0.0", 1);

        // Create a new interface using the builder and save the parameters.
        final ParcelFileDescriptor vpnInterface;

        builder.setSession(mServerName).setConfigureIntent(mConfigureIntent);
        synchronized (mService) {
            vpnInterface = builder.establish();
            if (mOnEstablishListener != null) {
                mOnEstablishListener.onEstablish(vpnInterface);
            }
        }
        Skywiremob.printString(getTag() + " New interface: " + vpnInterface);
        return vpnInterface;
    }

    private final String getTag() {
        return SkywireVPNConnection.class.getSimpleName() + "[" + mConnectionId + "]";
    }
}
