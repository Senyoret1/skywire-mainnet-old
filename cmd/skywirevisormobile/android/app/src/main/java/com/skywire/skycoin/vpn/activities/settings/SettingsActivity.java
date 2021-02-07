package com.skywire.skycoin.vpn.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.skywire.skycoin.vpn.activities.apps.AppsActivity;
import com.skywire.skycoin.vpn.extensible.ClickEvent;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.vpn.VPNGeneralPersistentData;

import java.util.HashSet;

public class SettingsActivity extends Fragment implements ClickEvent {
    private SettingsOption optionApps;
    private SettingsOption optionShowIp;
    private SettingsOption optionKillSwitch;
    private SettingsOption optionResetAfterErrors;
    private SettingsOption optionProtectBeforeConnecting;
    private SettingsOption optionStartOnBoot;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.activity_settings, container, true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        optionApps = view.findViewById(R.id.optionApps);
        optionShowIp = view.findViewById(R.id.optionShowIp);
        optionKillSwitch = view.findViewById(R.id.optionKillSwitch);
        optionResetAfterErrors = view.findViewById(R.id.optionResetAfterErrors);
        optionProtectBeforeConnecting = view.findViewById(R.id.optionProtectBeforeConnecting);
        optionStartOnBoot = view.findViewById(R.id.optionStartOnBoot);

        optionShowIp.setChecked(VPNGeneralPersistentData.getShowIpActivated());
        optionKillSwitch.setChecked(VPNGeneralPersistentData.getKillSwitchActivated());
        optionResetAfterErrors.setChecked(VPNGeneralPersistentData.getMustRestartVpn());
        optionProtectBeforeConnecting.setChecked(VPNGeneralPersistentData.getProtectBeforeConnected());
        optionStartOnBoot.setChecked(VPNGeneralPersistentData.getStartOnBoot());

        optionApps.setClickEventListener(this);
        optionShowIp.setClickEventListener(this);
        optionKillSwitch.setClickEventListener(this);
        optionResetAfterErrors.setClickEventListener(this);
        optionProtectBeforeConnecting.setClickEventListener(this);
        optionStartOnBoot.setClickEventListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        Globals.AppFilteringModes appsMode = VPNGeneralPersistentData.getAppsSelectionMode();
        if (appsMode == Globals.AppFilteringModes.PROTECT_ALL) {
            optionApps.setDescription(R.string.tmp_options_apps_description, null);
            optionApps.setChecked(false);
            optionApps.changeAlertIconVisibility(false);
        } else {
            HashSet<String> selectedApps = HelperFunctions.filterAvailableApps(VPNGeneralPersistentData.getAppList(new HashSet<>()));

            if (appsMode == Globals.AppFilteringModes.PROTECT_SELECTED) {
                optionApps.setDescription(R.string.tmp_options_apps_include_description, selectedApps.size() + "");
            } else if (appsMode == Globals.AppFilteringModes.IGNORE_SELECTED) {
                optionApps.setDescription(R.string.tmp_options_apps_exclude_description, selectedApps.size() + "");
            }

            optionApps.setChecked(true);
            optionApps.changeAlertIconVisibility(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
        super.onResume();
        HelperFunctions.closeActivityIfServiceRunning(this);
         */
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.optionApps) {
            Intent intent = new Intent(getContext(), AppsActivity.class);
            startActivity(intent);

            return;
        }

        ((SettingsOption)view).setChecked(!((SettingsOption)view).isChecked());

        if (view.getId() == R.id.optionShowIp) {
            VPNGeneralPersistentData.setShowIpActivated(((SettingsOption)view).isChecked());
        } else if (view.getId() == R.id.optionKillSwitch) {
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
