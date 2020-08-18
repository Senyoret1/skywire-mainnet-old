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
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.skywire.skycoin.vpn.helpers.App;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import java.util.Random;

import skywiremob.Skywiremob;

public class MainActivity extends Activity implements Handler.Callback, View.OnClickListener {

    private EditText mRemotePK;
    private EditText mPasscode;
    private TextView mStatus;

    private Handler serviceCommunicationHandler;
    private int communicationID;

    private final Object visorMx = new Object();
    private VisorRunnable visor = null;

    private SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());

    @Override
    public boolean handleMessage(Message msg) {
        int stateText = SkywireVPNService.getTextForState(msg.what);
        if (stateText != -1) {
            mStatus.setText(stateText);
            return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRemotePK = findViewById(R.id.editTextRemotePK);
        mPasscode = findViewById(R.id.editTextPasscode);
        mStatus = findViewById(R.id.textStatus);

        findViewById(R.id.buttonStart).setOnClickListener(this);
        findViewById(R.id.buttonStop).setOnClickListener(this);

        serviceCommunicationHandler = new Handler(this);
        communicationID = new Random().nextInt(Integer.MAX_VALUE);
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
        String remotePK = mRemotePK.getText().toString();
        String passcode = mPasscode.getText().toString();

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
        startService(getServiceIntent(false).setAction(SkywireVPNService.ACTION_DISCONNECT));
    }
}
