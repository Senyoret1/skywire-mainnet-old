package com.skywire.skycoin.vpn.vpn;

import com.skywire.skycoin.vpn.R;

public class VPNStates {
    public static int OFF = 1;
    public static int STARTING = 10;
    public static int CHECKING_CONNECTIVITY = 15;
    public static int WAITING_FOR_CONNECTIVITY = 16;
    public static int PREPARING_VISOR = 20;
    public static int PREPARING_VPN_CLIENT = 30;
    public static int FINAL_PREPARATIONS_FOR_VISOR = 35;
    public static int VISOR_READY = 40;
    public static int STARTING_VPN_CONNECTION = 50;
    public static int CONNECTED = 100;
    public static int RESTORING_VPN = 150;
    public static int DISCONNECTING = 200;
    public static int DISCONNECTED = 300;
    public static int ERROR = 400;
    public static int BLOCKING_ERROR = 410;

    public static class StateInfo {
        public final int state;
        public final boolean startedByTheSystem;
        public final String errorMsg;
        public final boolean stopRequested;

        public StateInfo(int state, boolean startedByTheSystem, String errorMsg, boolean stopRequested) {
            this.state = state;
            this.startedByTheSystem = startedByTheSystem;
            this.errorMsg = errorMsg;
            this.stopRequested = stopRequested;
        }
    }

    public static int getTextForState(int state) {
        if (state == OFF) {
            return R.string.vpn_state_off;
        } else if (state == STARTING) {
            return R.string.vpn_state_initializing;
        } else if (state == CHECKING_CONNECTIVITY) {
            return R.string.vpn_state_checking_connectivity;
        } else if (state == WAITING_FOR_CONNECTIVITY) {
            return R.string.vpn_state_waiting_connectivity;
        } else if (state == PREPARING_VISOR) {
            return R.string.vpn_state_starting_visor;
        } else if (state == PREPARING_VPN_CLIENT) {
            return R.string.vpn_state_starting_vpn_app;
        } else if (state == FINAL_PREPARATIONS_FOR_VISOR) {
            return R.string.vpn_state_additional_visor_initializations;
        } else if (state == VISOR_READY) {
            return R.string.vpn_state_connecting;
        } else if (state == STARTING_VPN_CONNECTION) {
            return R.string.vpn_state_connecting;
        } else if (state == CONNECTED) {
            return R.string.vpn_state_connected;
        } else if (state == RESTORING_VPN) {
            return R.string.vpn_state_restoring;
        } else if (state == DISCONNECTING) {
            return R.string.vpn_state_disconnecting;
        } else if (state == DISCONNECTED) {
            return R.string.vpn_state_disconnected;
        } else if (state == ERROR) {
            return R.string.vpn_state_error;
        } else if (state == BLOCKING_ERROR) {
            return R.string.vpn_state_blocking_error;
        }

        return -1;
    }
}
