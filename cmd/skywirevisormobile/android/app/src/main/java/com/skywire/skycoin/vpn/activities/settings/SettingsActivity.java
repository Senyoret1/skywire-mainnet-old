package com.skywire.skycoin.vpn.activities.settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.skywire.skycoin.vpn.HelperFunctions;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.vpn.VPNPersistentData;

public class SettingsActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    private CheckBox checkBoxKillSwitch;
    private CheckBox checkBoxStartOnBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        checkBoxKillSwitch = findViewById(R.id.checkBoxKillSwitch);
        checkBoxStartOnBoot = findViewById(R.id.checkBoxStartOnBoot);

        checkBoxKillSwitch.setChecked(VPNPersistentData.getKillSwitchActivated());
        checkBoxStartOnBoot.setChecked(VPNPersistentData.getStartOnBoot());

        checkBoxKillSwitch.setOnCheckedChangeListener(this);
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
        } else {
            VPNPersistentData.setStartOnBoot(isChecked);
        }
    }
}
