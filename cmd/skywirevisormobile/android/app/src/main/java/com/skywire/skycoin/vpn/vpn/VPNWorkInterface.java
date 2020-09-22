package com.skywire.skycoin.vpn.vpn;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.Globals;
import com.skywire.skycoin.vpn.HelperFunctions;
import com.skywire.skycoin.vpn.R;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

import skywiremob.Skywiremob;

public class VPNWorkInterface implements Closeable {
    private final VpnService service;
    private final PendingIntent configureIntent;

    private ParcelFileDescriptor vpnInterface = null;

    private FileInputStream inStream = null;
    private FileOutputStream outStream = null;

    private boolean createdForCleaning;

    public VPNWorkInterface(
        VpnService service,
        PendingIntent configureIntent,
        boolean createForCleaning
    ) {
        this.service = service;
        this.configureIntent = configureIntent;
        createdForCleaning = createForCleaning;
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

    public boolean alreadyConfigured() {
        return vpnInterface != null;
    }

    public void configure() throws Exception {
        ParcelFileDescriptor oldVpnInterface = null;
        if (vpnInterface != null) {
            oldVpnInterface = vpnInterface;
        }

        // Configure a builder while parsing the parameters.
        VpnService.Builder builder = service.new Builder();
        builder.setMtu((short)Skywiremob.getMTU());
        if (!createdForCleaning) {
            Skywiremob.printString("TUN IP: " + Skywiremob.tunip());
            builder.addAddress(Skywiremob.tunip(), (int) Skywiremob.getTUNIPPrefix());
        } else {
            builder.addAddress("8.8.8.8", 1);
        }
        builder.addDnsServer("8.8.8.8");
        //builder.addDnsServer("192.168.1.1");
        builder.addRoute("0.0.0.0", 1);
        builder.addRoute("128.0.0.0", 1);
        builder.setBlocking(true);

        boolean errorIgnoringApps = false;
        if (!createdForCleaning) {
            String upperCaseAppPackage = App.getContext().getPackageName().toUpperCase();
            Globals.AppFilteringModes appsSelectionMode = VPNPersistentData.getAppsSelectionMode();

            if (appsSelectionMode != Globals.AppFilteringModes.PROTECT_ALL) {
                for (String packageName : HelperFunctions.filterAvailableApps(VPNPersistentData.getAppList(new HashSet<>()))) {
                    try {
                        if (appsSelectionMode == Globals.AppFilteringModes.PROTECT_SELECTED) {
                            if (!upperCaseAppPackage.equals(packageName.toUpperCase())) {
                                builder.addAllowedApplication(packageName);
                            }
                        } else {
                            if (!upperCaseAppPackage.equals(packageName.toUpperCase())) {
                                builder.addDisallowedApplication(packageName);
                            }
                        }
                    } catch (Exception e) {
                        errorIgnoringApps = true;
                        HelperFunctions.logError("Unable to add " + packageName + " to the VPN service", e);
                        break;
                    }
                }
            }

            if (!errorIgnoringApps) {
                try {
                    if (appsSelectionMode != Globals.AppFilteringModes.PROTECT_SELECTED) {
                        builder.addDisallowedApplication(App.getContext().getPackageName());
                    }
                } catch (Exception e) {
                    errorIgnoringApps = true;
                    HelperFunctions.logError("Unable to add VPN app rule to the VPN service", e);
                }
            }
        } else {
            builder.addAllowedApplication(App.getContext().getPackageName());
        }

        if (errorIgnoringApps) {
            throw new Exception(App.getContext().getString(R.string.vpn_service_configuring_app_rules_error));
        }

        // Create a new interface using the builder and save the parameters.
        builder.setConfigureIntent(configureIntent);
        synchronized (service) {
            vpnInterface = builder.establish();
        }
        Skywiremob.printString("New interface: " + vpnInterface);

        if (oldVpnInterface != null) {
            oldVpnInterface.close();
        }
    }

    public FileInputStream getInputStream() {
        if (inStream == null) {
            inStream = new FileInputStream(vpnInterface.getFileDescriptor());
        }
        return inStream;
    }

    public FileOutputStream getOutputStream() {
        if (outStream == null) {
            outStream = new FileOutputStream(vpnInterface.getFileDescriptor());
        }
        return outStream;
    }
}
