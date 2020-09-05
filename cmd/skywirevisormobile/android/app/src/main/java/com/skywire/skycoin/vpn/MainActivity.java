package com.skywire.skycoin.vpn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.skywire.skycoin.vpn.helpers.App;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import java.util.Random;

import skywiremob.Skywiremob;

public class MainActivity extends Activity implements Handler.Callback, View.OnClickListener {

    private EditText editTextRemotePK;
    private EditText editTextPasscode;
    private Button buttonStart;
    private Button buttonStop;
    private TextView textStatus;
    private TextView textFinishAlert;

    private Handler serviceCommunicationHandler;
    private int communicationID;

    private boolean showingError = false;

    private SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what != SkywireVPNService.States.ERROR && msg.what != SkywireVPNService.States.DISCONNECTED) {
            int stateText = SkywireVPNService.getTextForState(msg.what);
            if (stateText != -1) {
                textStatus.setText(stateText);
                return true;
            }
        } else if (msg.what == SkywireVPNService.States.DISCONNECTED) {
            if (!showingError) {
                textStatus.setText(R.string.vpn_state_disconnected);
            }

            displayInitialState(false);

            return true;
        } else {
            textStatus.setText(msg.getData().getString(SkywireVPNService.ERROR_MSG_PARAM));
            displayErrorState();

            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextRemotePK = findViewById(R.id.editTextRemotePK);
        editTextPasscode = findViewById(R.id.editTextPasscode);
        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);
        textStatus = findViewById(R.id.textStatus);
        textFinishAlert = findViewById(R.id.textFinishAlert);

        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        serviceCommunicationHandler = new Handler(this);
        communicationID = new Random().nextInt(Integer.MAX_VALUE);

        String savedPk = settings.getString(Globals.StorageVars.SERVER_PK, null);
        String savedPassword = settings.getString(Globals.StorageVars.SERVER_PASSWORD, null);

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

        displayInitialState(true);

        if (HelperFunctions.isServiceRunning()) {
            displayWorkingState();
            onActivityResult(0, RESULT_OK, null);
        } else {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());
            String savedError = settings.getString(Globals.StorageVars.LAST_ERROR, null);

            if (savedError != null) {
                textStatus.setText(savedError);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (HelperFunctions.isServiceRunning()) {
            startService(getServiceIntent(true).setAction(SkywireVPNService.ACTION_STOP_COMUNNICATION));
        }
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
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(getServiceIntent(true).setAction(SkywireVPNService.ACTION_CONNECT));
            } else {
                startService(getServiceIntent(true).setAction(SkywireVPNService.ACTION_CONNECT));
            }
        } else {
            displayInitialState(true);
        }
    }

    private Intent getServiceIntent(boolean IncludeExtras) {
        Intent response = new Intent(this, SkywireVPNService.class);
        if (IncludeExtras) {
            response.putExtra("Messenger", new Messenger(serviceCommunicationHandler));
            response.putExtra("ID", communicationID);
        }
        return response;
    }

    private void start() {
        displayWorkingState();

        String remotePK = editTextRemotePK.getText().toString();
        String passcode = editTextPasscode.getText().toString();

        String err = Skywiremob.isPKValid(remotePK);
        if (!err.isEmpty()) {
            HelperFunctions.showToast("Invalid credentials: " + err, false);
            return;
        } else {
            Skywiremob.printString("PK is correct");
        }

        settings.edit()
            .putString(Globals.StorageVars.SERVER_PK, remotePK)
            .putString(Globals.StorageVars.SERVER_PASSWORD, passcode)
            .apply();

        Intent intent = VpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    private void stop() {
        buttonStop.setEnabled(false);
        startService(getServiceIntent(false).setAction(SkywireVPNService.ACTION_DISCONNECT));
    }

    private void displayInitialState(boolean restartStatusText) {
        if (restartStatusText) {
            textStatus.setText("");
        }

        showingError = false;
        editTextRemotePK.setEnabled(true);
        editTextPasscode.setEnabled(true);
        buttonStart.setEnabled(true);
        buttonStop.setEnabled(false);
        textFinishAlert.setVisibility(View.GONE);
    }

    private void displayWorkingState() {
        editTextRemotePK.setEnabled(false);
        editTextPasscode.setEnabled(false);
        buttonStart.setEnabled(false);
        buttonStop.setEnabled(true);
        textFinishAlert.setVisibility(View.GONE);
    }

    private void displayErrorState() {
        showingError = true;
        editTextRemotePK.setEnabled(false);
        editTextPasscode.setEnabled(false);
        buttonStart.setEnabled(false);
        buttonStop.setEnabled(false);
        textFinishAlert.setVisibility(View.VISIBLE);
    }
}
