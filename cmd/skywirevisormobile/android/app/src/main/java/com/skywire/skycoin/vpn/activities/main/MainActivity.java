package com.skywire.skycoin.vpn.activities.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.VPNCoordinator;
import com.skywire.skycoin.vpn.VPNPersistentData;
import com.skywire.skycoin.vpn.VPNStates;
import com.skywire.skycoin.vpn.activities.apps.AppsActivity;
import com.skywire.skycoin.vpn.activities.servers.ServersActivity;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import java.util.HashSet;

import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends Activity implements View.OnClickListener {

    private EditText editTextRemotePK;
    private EditText editTextPasscode;
    private Button buttonStart;
    private Button buttonStop;
    private Button buttonSelect;
    private Button buttonApps;
    private TextView textLastError1;
    private TextView textLastError2;
    private TextView textStatus;
    private TextView textFinishAlert;
    private TextView textStopAlert;

    private Disposable serviceSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextRemotePK = findViewById(R.id.editTextRemotePK);
        editTextPasscode = findViewById(R.id.editTextPasscode);
        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);
        buttonSelect = findViewById(R.id.buttonSelect);
        buttonApps = findViewById(R.id.buttonApps);
        textStatus = findViewById(R.id.textStatus);
        textFinishAlert = findViewById(R.id.textFinishAlert);
        textLastError1 = findViewById(R.id.textLastError1);
        textLastError2 = findViewById(R.id.textLastError2);
        textStopAlert = findViewById(R.id.textStopAlert);

        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        buttonSelect.setOnClickListener(this);
        buttonApps.setOnClickListener(this);

        String savedPk = VPNPersistentData.getPublicKey(null);
        String savedPassword = VPNPersistentData.getPassword(null);

        if (savedPk != null && savedPassword != null) {
            editTextRemotePK.setText(savedPk);
            editTextPasscode.setText(savedPassword);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        editTextRemotePK.setText(savedInstanceState.getString("pk"));
        editTextPasscode.setText(savedInstanceState.getString("password"));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("pk", editTextRemotePK.getText().toString());
        savedInstanceState.putString("password", editTextPasscode.getText().toString());
    }

    @Override
    protected void onStart() {
        super.onStart();

        displayInitialState();

        serviceSubscription = VPNCoordinator.getInstance().getEventsObservable().subscribe(
            state -> {
                if (state.state < 10) {
                    displayInitialState();
                } else if (state.state != VPNStates.ERROR && state.state != VPNStates.BLOCKING_ERROR && state.state != VPNStates.DISCONNECTED) {
                    int stateText = VPNStates.getTextForState(state.state);

                    displayWorkingState();

                    if (state.startedByTheSystem) {
                        this.buttonStop.setEnabled(false);
                        textStopAlert.setVisibility(View.VISIBLE);
                    }

                    if (state.stopRequested) {
                        this.buttonStop.setEnabled(false);
                    }

                    if (stateText != -1) {
                        textStatus.setText(stateText);
                    }
                } else if (state.state == VPNStates.DISCONNECTED) {
                    textStatus.setText(R.string.vpn_state_disconnected);
                    displayInitialState();
                } else {
                    textStatus.setText(VPNStates.getTextForState(state.state));
                    displayErrorState(state.stopRequested);
                }
            }
        );
    }

    @Override
    protected void onStop() {
        super.onStop();

        serviceSubscription.dispose();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonStart:
                start();
                break;
            case R.id.buttonStop:
                stop();
                break;
            case R.id.buttonSelect:
                selectServer();
                break;
            case R.id.buttonApps:
                selectApps();
                break;
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == VPNCoordinator.VPN_PREPARATION_REQUEST_CODE) {
            VPNCoordinator.getInstance().onActivityResult(request, result, data);
        } else if (request == 1 && data != null) {
            String address = data.getStringExtra(ServersActivity.ADDRESS_DATA_PARAM);
            if (address != null) {
                editTextRemotePK.setText(address);
                editTextPasscode.setText("");
            }
        }
    }

    private void start() {
        Globals.AppFilteringModes selectedMode = VPNPersistentData.getAppsSelectionMode();
        if (selectedMode != Globals.AppFilteringModes.PROTECT_ALL) {
            HashSet<String> selectedApps = HelperFunctions.filterAvailableApps(VPNPersistentData.getAppList(new HashSet<>()));

            if (selectedApps.size() == 0) {
                if (selectedMode == Globals.AppFilteringModes.PROTECT_SELECTED) {
                    HelperFunctions.showToast(getString(R.string.vpn_no_apps_to_protect_warning), false);
                } else {
                    HelperFunctions.showToast(getString(R.string.vpn_no_apps_to_ignore_warning), false);
                }
            }
        }

        VPNCoordinator.getInstance().startVPN(
            this,
            editTextRemotePK.getText().toString(),
            editTextPasscode.getText().toString()
        );
    }

    private void stop() {
        VPNCoordinator.getInstance().stopVPN();
    }

    private void selectServer() {
        Intent intent = new Intent(this, ServersActivity.class);
        startActivityForResult(intent, 1);
    }

    private void selectApps() {
        Intent intent = new Intent(this, AppsActivity.class);
        startActivity(intent);
    }

    private void displayInitialState() {
        textStatus.setText(R.string.vpn_state_off);

        editTextRemotePK.setEnabled(true);
        editTextPasscode.setEnabled(true);
        buttonStart.setEnabled(true);
        buttonStop.setEnabled(false);
        buttonSelect.setEnabled(true);
        buttonApps.setEnabled(true);
        textFinishAlert.setVisibility(View.GONE);
        textStopAlert.setVisibility(View.GONE);

        String lastError = VPNPersistentData.getLastError(null);
        if (lastError != null) {
            textLastError1.setVisibility(View.VISIBLE);
            textLastError2.setVisibility(View.VISIBLE);
            textLastError2.setText(lastError);
        } else {
            textLastError1.setVisibility(View.GONE);
            textLastError2.setVisibility(View.GONE);
        }
    }

    private void displayWorkingState() {
        editTextRemotePK.setEnabled(false);
        editTextPasscode.setEnabled(false);
        buttonStart.setEnabled(false);
        buttonStop.setEnabled(true);
        buttonSelect.setEnabled(false);
        buttonApps.setEnabled(false);
        textFinishAlert.setVisibility(View.GONE);
        textStopAlert.setVisibility(View.GONE);

        textLastError1.setVisibility(View.GONE);
        textLastError2.setVisibility(View.GONE);
    }

    private void displayErrorState(boolean stopRequested) {
        editTextRemotePK.setEnabled(false);
        editTextPasscode.setEnabled(false);
        buttonStart.setEnabled(false);
        buttonStop.setEnabled(!stopRequested);
        buttonSelect.setEnabled(false);
        buttonApps.setEnabled(false);
        textFinishAlert.setVisibility(stopRequested ? View.VISIBLE : View.GONE);
        textStopAlert.setVisibility(View.GONE);

        textLastError1.setVisibility(View.VISIBLE);
        textLastError2.setVisibility(View.VISIBLE);

        String lastError = VPNPersistentData.getLastError(null);
        textLastError2.setText(lastError);
    }
}
