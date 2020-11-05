package com.skywire.skycoin.vpn.activities.settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.vpn.VPNPersistentData;

public class SettingsActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    private CheckBox checkBoxKillSwitch;
    private CheckBox checkBoxResetAfterErrors;
    private CheckBox checkBoxProtectBeforeConnecting;
    private CheckBox checkBoxStartOnBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        checkBoxKillSwitch = findViewById(R.id.checkBoxKillSwitch);
        checkBoxResetAfterErrors = findViewById(R.id.checkBoxResetAfterErrors);
        checkBoxProtectBeforeConnecting = findViewById(R.id.checkBoxProtectBeforeConnecting);
        checkBoxStartOnBoot = findViewById(R.id.checkBoxStartOnBoot);

        checkBoxKillSwitch.setChecked(VPNPersistentData.getKillSwitchActivated());
        checkBoxResetAfterErrors.setChecked(VPNPersistentData.getMustRestartVpn());
        checkBoxProtectBeforeConnecting.setChecked(VPNPersistentData.getProtectBeforeConnected());
        checkBoxStartOnBoot.setChecked(VPNPersistentData.getStartOnBoot());

        checkBoxKillSwitch.setOnCheckedChangeListener(this);
        checkBoxResetAfterErrors.setOnCheckedChangeListener(this);
        checkBoxProtectBeforeConnecting.setOnCheckedChangeListener(this);
        checkBoxStartOnBoot.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HelperFunctions.closeActivityIfServiceRunning(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.checkBoxKillSwitch) {
            VPNPersistentData.setKillSwitchActivated(isChecked);
        } else if (buttonView.getId() == R.id.checkBoxResetAfterErrors) {
            VPNPersistentData.setMustRestartVpn(isChecked);
        } else if (buttonView.getId() == R.id.checkBoxProtectBeforeConnecting) {
            VPNPersistentData.setProtectBeforeConnected(isChecked);
        } else {
            VPNPersistentData.setStartOnBoot(isChecked);
        }
    }
}
