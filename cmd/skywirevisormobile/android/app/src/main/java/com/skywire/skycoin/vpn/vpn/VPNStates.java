package com.skywire.skycoin.vpn.vpn;

import com.skywire.skycoin.vpn.R;

/**
 * Helper class with the possible states of the VPN service.
 *
 * The states are numeric constants, similar to how http status codes work, to be able to identify
 * state groups just by numeric ranges. The ranges are:
 *
 * State < 10: the service is not running.
 *
 * 10 =< State < 100: The VPN connection is being prepared.
 *
 * 100 =< State < 150: The VPN connection has been made and the internet connectivity should
 * be protected and working.
 *
 * 150 =< State < 200: Temporal errors with the VPN connection.
 *
 * 200 =< State < 300: Closing the VPN connection/service.
 *
 * 300 =< State < 400: VPN connection/service closed.
 *
 * State >= 400 : An error occurred.
 */
public class VPNStates {
    /**
     * The service is off.
     */
    public static int OFF = 1;
    /**
     * Starting the service.
     */
    public static int STARTING = 10;
    /**
     * Waiting for the visor to be completely stopped before starting it again.
     */
    public static int WAITING_PREVIOUS_INSTANCE_STOP = 12;
    /**
     * Checking for the first time if the device has internet connectivity.
     */
    public static int CHECKING_CONNECTIVITY = 15;
    /**
     * No internet connectivity was found and the service is checking again periodically.
     */
    public static int WAITING_FOR_CONNECTIVITY = 16;
    /**
     * Starting the Skywire visor.
     */
    public static int PREPARING_VISOR = 20;
    /**
     * Starting the VPN client, which is part of Skywiremob and running as part of the visor.
     */
    public static int PREPARING_VPN_CLIENT = 30;
    /**
     * Making final preparations for the VPN client, like performing the handshake and start serving.
     */
    public static int FINAL_PREPARATIONS_FOR_VISOR = 35;
    /**
     * The visor and VPN client are ready. Preparations may be needed in the app side.
     */
    public static int VISOR_READY = 40;
    /**
     * The VPN connection has been fully established and secure internet connectivity should
     * be available.
     */
    public static int CONNECTED = 100;
    /**
     * There was an error with the VPN connection and it is being restored automatically.
     */
    public static int RESTORING_VPN = 150;
    /**
     * There was an error and the whole VPN service is being restored automatically.
     */
    public static int RESTORING_SERVICE = 155;
    /**
     * The VPN service is being stopped.
     */
    public static int DISCONNECTING = 200;
    /**
     * The VPN service has been stopped.
     */
    public static int DISCONNECTED = 300;
    /**
     * There has been an error, the VPN connection is not available and the service is
     * being stopped.
     */
    public static int ERROR = 400;
    /**
     * There has been and error and the VPN connection is not available. The network will remain
     * blocked until the user stops the service manually.
     */
    public static int BLOCKING_ERROR = 410;

    /**
     * Class with details about the state of the VPN service.
     */
    public static class StateInfo {
        /**
         * Current state of the service, it is one of the  constants defined in the VPNStates class.
         */
        public final int state;
        /**
         * If the service was started by the OS, which means that the OS is responsible for
         * stopping it.
         */
        public final boolean startedByTheSystem;
        /**
         * Error message, only if the current state indicates an error.
         */
        public final String errorMsg;
        /**
         * If the user already requested the service to be stopped.
         */
        public final boolean stopRequested;

        public StateInfo(int state, boolean startedByTheSystem, String errorMsg, boolean stopRequested) {
            this.state = state;
            this.startedByTheSystem = startedByTheSystem;
            this.errorMsg = errorMsg;
            this.stopRequested = stopRequested;
        }
    }

    /**
     * Allows to get the resource ID of the string with the message identifying a state of the
     * VPN service. If no resource is found for the state, -1 is returned.
     */
    public static int getTextForState(int state) {
        if (state == OFF) {
            return R.string.vpn_state_off;
        } else if (state == STARTING) {
            return R.string.vpn_state_initializing;
        } else if (state == WAITING_PREVIOUS_INSTANCE_STOP) {
            return R.string.vpn_state_waiting_previous_instance_stop;
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
        } else if (state == CONNECTED) {
            return R.string.vpn_state_connected;
        } else if (state == RESTORING_VPN) {
            return R.string.vpn_state_restoring;
        } else if (state == RESTORING_SERVICE) {
            return R.string.vpn_state_restoring_service;
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
