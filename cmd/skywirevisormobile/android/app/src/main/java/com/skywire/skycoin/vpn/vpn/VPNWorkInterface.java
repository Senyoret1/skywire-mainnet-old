package com.skywire.skycoin.vpn.vpn;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.skywire.skycoin.vpn.Globals;
import com.skywire.skycoin.vpn.HelperFunctions;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;

import skywiremob.Skywiremob;

public class VPNWorkInterface implements Closeable {
    private final VpnService service;
    private final PendingIntent configureIntent;

    private ParcelFileDescriptor vpnInterface = null;

    public VPNWorkInterface(
        VpnService service,
        PendingIntent configureIntent
    ) {
        this.service = service;
        this.configureIntent = configureIntent;
    }

    @Override
    public void close() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                vpnInterface = null;
            } catch (IOException e) {
                HelperFunctions.logError("Unable to close interface", e);
            }
        }
    }

    public ParcelFileDescriptor getVpnInterface() {
        return vpnInterface;
    }

    public boolean alreadyConfigured() {
        return vpnInterface != null;
    }

    public void configure() throws IllegalArgumentException {
        if (vpnInterface != null) {
            return;
        }

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

        Globals.AppFilteringModes appsSelectionMode = VPNPersistentData.getAppsSelectionMode();
        if (appsSelectionMode != Globals.AppFilteringModes.PROTECT_ALL) {
            for (String packageName : HelperFunctions.filterAvailableApps(VPNPersistentData.getAppList(new HashSet<>()))) {
                try {
                    if (appsSelectionMode == Globals.AppFilteringModes.PROTECT_SELECTED) {
                        builder.addAllowedApplication(packageName);
                    } else {
                        builder.addDisallowedApplication(packageName);
                    }
                } catch (Exception e){
                    HelperFunctions.logError("Unable to add " + packageName + " to the VPN service", e);
                }
            }
        }

        // Create a new interface using the builder and save the parameters.
        builder.setConfigureIntent(configureIntent);
        synchronized (service) {
            vpnInterface = builder.establish();
        }
        Skywiremob.printString("New interface: " + vpnInterface);
    }
}
