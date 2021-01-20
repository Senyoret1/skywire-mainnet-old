package com.skywire.skycoin.vpn.activities.settings;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.skywire.skycoin.vpn.extensible.ClickEvent;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.vpn.VPNGeneralPersistentData;

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

        optionKillSwitch.setChecked(VPNGeneralPersistentData.getKillSwitchActivated());
        optionResetAfterErrors.setChecked(VPNGeneralPersistentData.getMustRestartVpn());
        optionProtectBeforeConnecting.setChecked(VPNGeneralPersistentData.getProtectBeforeConnected());
        optionStartOnBoot.setChecked(VPNGeneralPersistentData.getStartOnBoot());

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
            VPNGeneralPersistentData.setKillSwitchActivated(((SettingsOption)view).isChecked());
        } else if (view.getId() == R.id.optionResetAfterErrors) {
            VPNGeneralPersistentData.setMustRestartVpn(((SettingsOption)view).isChecked());
        } else if (view.getId() == R.id.optionProtectBeforeConnecting) {
            VPNGeneralPersistentData.setProtectBeforeConnected(((SettingsOption)view).isChecked());
        } else {
            VPNGeneralPersistentData.setStartOnBoot(((SettingsOption)view).isChecked());
        }
    }
}
