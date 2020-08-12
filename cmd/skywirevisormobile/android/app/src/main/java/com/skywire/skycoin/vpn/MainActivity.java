package com.skywire.skycoin.vpn;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;

import com.skywire.skycoin.vpn.helpers.App;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;

import skywiremob.Skywiremob;

public class MainActivity extends Activity implements Handler.Callback, View.OnClickListener {

    private EditText mRemotePK;
    private EditText mPasscode;

    private final Object visorMx = new Object();
    private VisorRunnable visor = null;

    private SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(App.getContext());

    @Override
    public boolean handleMessage(Message msg) {
        String err = msg.getData().getString("text");
        HelperFunctions.showToast(err, false);
        return false;
    }

    public void startVPNService() {
        Intent intent = VpnService.prepare(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRemotePK = findViewById(R.id.editTextRemotePK);
        mPasscode = findViewById(R.id.editTextPasscode);

        findViewById(R.id.buttonStart).setOnClickListener(this);
        findViewById(R.id.buttonStop).setOnClickListener(this);
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
                startForegroundService(getServiceIntent().setAction(SkywireVPNService.ACTION_CONNECT));
            } else {
                startService(getServiceIntent().setAction(SkywireVPNService.ACTION_CONNECT));
            }
        }
    }

    private Intent getServiceIntent() {
        return new Intent(this, SkywireVPNService.class);
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
            .putString("remotePK", remotePK)
            .putString("passcode", passcode)
            .apply();

        synchronized (visorMx) {
            // TODO: the service may be running even if visor != null, so this have to be improved.
            if (visor != null) {
                visor.stopVisor();
                visor = null;
                stopService(getServiceIntent().setAction(SkywireVPNService.ACTION_DISCONNECT));
            }

            visor = new VisorRunnable(MainActivity.this, remotePK, passcode);

            new Thread(visor).start();
        }
    }

    private void stop() {
        startService(getServiceIntent().setAction(SkywireVPNService.ACTION_DISCONNECT));

        synchronized (visorMx) {
            if (visor != null) {
                visor.stopVisor();
            }
        }
    }
}
