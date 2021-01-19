package com.skywire.skycoin.vpn.activities.settings;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.skywire.skycoin.vpn.extensible.ClickEvent;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.vpn.VPNPersistentData;

public class SettingsActivity extends AppCompatActivity implements ClickEvent {
    private SettingsOption optionKillSwitch;
    private SettingsOption optionResetAfterErrors;
    private SettingsOption optionProtectBeforeConnecting;
    private SettingsOption optionStartOnBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        optionKillSwitch = findViewById(R.id.optionKillSwitch);
        optionResetAfterErrors = findViewById(R.id.optionResetAfterErrors);
        optionProtectBeforeConnecting = findViewById(R.id.optionProtectBeforeConnecting);
        optionStartOnBoot = findViewById(R.id.optionStartOnBoot);

        optionKillSwitch.setChecked(VPNPersistentData.getKillSwitchActivated());
        optionResetAfterErrors.setChecked(VPNPersistentData.getMustRestartVpn());
        optionProtectBeforeConnecting.setChecked(VPNPersistentData.getProtectBeforeConnected());
        optionStartOnBoot.setChecked(VPNPersistentData.getStartOnBoot());

        optionKillSwitch.setClickEventListener(this);
        optionResetAfterErrors.setClickEventListener(this);
        optionProtectBeforeConnecting.setClickEventListener(this);
        optionStartOnBoot.setClickEventListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HelperFunctions.closeActivityIfServiceRunning(this);
    }

    @Override
    public void onClick(View view) {
        ((SettingsOption)view).setChecked(!((SettingsOption)view).isChecked());

        if (view.getId() == R.id.optionKillSwitch) {
            VPNPersistentData.setKillSwitchActivated(((SettingsOption)view).isChecked());
        } else if (view.getId() == R.id.optionResetAfterErrors) {
            VPNPersistentData.setMustRestartVpn(((SettingsOption)view).isChecked());
        } else if (view.getId() == R.id.optionProtectBeforeConnecting) {
            VPNPersistentData.setProtectBeforeConnected(((SettingsOption)view).isChecked());
        } else {
            VPNPersistentData.setStartOnBoot(((SettingsOption)view).isChecked());
        }
    }
}
