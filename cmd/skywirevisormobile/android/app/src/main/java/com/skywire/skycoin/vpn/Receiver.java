package com.skywire.skycoin.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.skywire.skycoin.vpn.vpn.VPNCoordinator;
import com.skywire.skycoin.vpn.vpn.VPNPersistentData;

public class Receiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1) {
        if (VPNPersistentData.getStartOnBoot() && !VPNCoordinator.getInstance().isServiceRunning()) {
            VPNCoordinator.getInstance().activateAutostart();
        }
    }
}
