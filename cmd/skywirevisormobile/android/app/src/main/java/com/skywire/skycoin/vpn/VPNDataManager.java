package com.skywire.skycoin.vpn;

import android.os.ParcelFileDescriptor;

import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import skywiremob.Skywiremob;

public class VPNDataManager {
    private FileOutputStream out;
    private DatagramChannel tunnel;

    /**
     * Time between polling the VPN interface for new traffic, since it's non-blocking.
     *
     * TODO: really don't do this; a blocking read on another thread is much cleaner.
     */
    private static final long IDLE_INTERVAL_MS = TimeUnit.MILLISECONDS.toMillis(100);

    static public Observable<Integer> createObservable(ParcelFileDescriptor iface, DatagramChannel tunnel, boolean forSending) {
        return Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            // Packets to be sent are queued in this input stream.
            FileInputStream inStream = null;
            // Packets received need to be written to this output stream.
            FileOutputStream outStream = null;
            if (forSending) {
                inStream = new FileInputStream(iface.getFileDescriptor());
            } else {
                outStream = new FileOutputStream(iface.getFileDescriptor());
            }
            final FileInputStream in = inStream;
            final FileOutputStream out = outStream;

            ByteBuffer packet = ByteBuffer.allocate(Short.MAX_VALUE);

            try {
                boolean idle;
                while (!emitter.isDisposed()) {
                    idle = true;

                    if (forSending) {
                        // Read the outgoing packet from the input stream.
                        int length = in.read(packet.array());
                        if (length > 0) {
                            // Write the outgoing packet to the tunnel.
                            packet.limit(length);
                            tunnel.write(packet);
                            packet.clear();
                            idle = false;
                        }
                    }

                    if (!forSending) {
                        int length = tunnel.read(packet);
                        if (length > 0) {
                            // Ignore control messages, which start with zero.
                            if (packet.get(0) != 0) {
                                // Write the incoming packet to the output stream.
                                out.write(packet.array(), 0, length);
                            }
                            packet.clear();
                            idle = false;
                        }
                    }

                    if (idle) {
                        Thread.sleep(IDLE_INTERVAL_MS);
                    }
                }
            } catch (Exception e) {
                HelperFunctions.logError("EXCEPTION IN VPNDataManager", e);
            }

            emitter.onComplete();
        });
    }
}
