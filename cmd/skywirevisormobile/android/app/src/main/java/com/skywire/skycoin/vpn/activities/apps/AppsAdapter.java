package com.skywire.skycoin.vpn.activities.apps;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.VPNPersistentData;
import com.skywire.skycoin.vpn.helpers.ClickWithIndexEvent;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.helpers.ListViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<ListViewHolder<View>> implements ClickWithIndexEvent<Void> {
    private Context context;
    private List<ResolveInfo> appList;

    private HashSet<String> selectedApps;
    private Globals.AppFilteringModes selectedOption;

    private HashMap<Integer, Integer> optionTexts = new HashMap<>();
    private ArrayList<AppListOptionButton> optionButtons = new ArrayList<>();
    private ArrayList<AppListButton> appButtons = new ArrayList<>();

    public AppsAdapter(Context context) {
        this.context = context;

        selectedApps = VPNPersistentData.getAppList(new HashSet<>());
        changeSelectedOption(VPNPersistentData.getAppsSelectionMode());

        appList = HelperFunctions.getDeviceAppsList();

        optionTexts.put(0, R.string.tmp_select_apps_protect_all_button);
        optionTexts.put(1, R.string.tmp_select_apps_protect_selected_button);
        optionTexts.put(2, R.string.tmp_select_apps_unprotect_selected_button);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 3) {
            return 0;
        }

        return 1;
    }

    @Override
    public ListViewHolder<View> onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            AppListOptionButton option = new AppListOptionButton(context);
            option.setClickWithIndexEventListener(this);
            optionButtons.add(option);

            return new ListViewHolder<>(option);
        }

        AppListButton view = new AppListButton(context);
        view.setClickWithIndexEventListener(this);
        view.setEnabled(selectedOption != Globals.AppFilteringModes.PROTECT_ALL);
        appButtons.add(view);

        return new ListViewHolder<>(view);
    }

    @Override
    public void onBindViewHolder(ListViewHolder<View> holder, int position) {
        if (position < 3) {
            boolean showChecked = false;
            if (position == 0 && selectedOption == Globals.AppFilteringModes.PROTECT_ALL) { showChecked = true; }
            if (position == 1 && selectedOption == Globals.AppFilteringModes.PROTECT_SELECTED) { showChecked = true; }
            if (position == 2 && selectedOption == Globals.AppFilteringModes.IGNORE_SELECTED) { showChecked = true; }

            int optionText = optionTexts.get(position);
            ((AppListOptionButton)(holder.getButtonView())).setIndex(position);
            ((AppListOptionButton)(holder.getButtonView())).changeData(optionText);
            ((AppListOptionButton)(holder.getButtonView())).setChecked(showChecked);

            return;
        }

        String element = appList.get(position - 3).activityInfo.packageName;
        ((AppListButton)(holder.getButtonView())).setIndex(position);
        ((AppListButton)(holder.getButtonView())).changeData(appList.get(position - 3));
        ((AppListButton)(holder.getButtonView())).setChecked(selectedApps.contains(element));
    }

    @Override
    public int getItemCount() {
        return appList.size() + 3;
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (index < 3) {
            if (index == 0) {
                changeSelectedOption(Globals.AppFilteringModes.PROTECT_ALL);
            } else if (index == 1) {
                changeSelectedOption(Globals.AppFilteringModes.PROTECT_SELECTED);
            } else if (index == 2) {
                changeSelectedOption(Globals.AppFilteringModes.IGNORE_SELECTED);
            }
        } else {
            processAppClicked(index);
        }
    }

    private void changeSelectedOption(Globals.AppFilteringModes option) {
        if (option != selectedOption) {
            if (option == Globals.AppFilteringModes.PROTECT_ALL) {
                for (AppListButton appButton : appButtons) {
                    appButton.setEnabled(false);
                }
            } else if (selectedOption == Globals.AppFilteringModes.PROTECT_ALL) {
                for (AppListButton appButton : appButtons) {
                    appButton.setEnabled(true);
                }
            }

            selectedOption = option;
            VPNPersistentData.setAppsSelectionMode(selectedOption);

            for (AppListOptionButton optionButton : optionButtons) {
                optionButton.setChecked(
                    (optionButton.getIndex() == 0 && selectedOption == Globals.AppFilteringModes.PROTECT_ALL) ||
                    (optionButton.getIndex() == 1 && selectedOption == Globals.AppFilteringModes.PROTECT_SELECTED) ||
                    (optionButton.getIndex() == 2 && selectedOption == Globals.AppFilteringModes.IGNORE_SELECTED)
                );
            }
        }
    }

    private void processAppClicked(int index) {
        String element = appList.get(index - 3).activityInfo.packageName;
        if (selectedApps.contains(element)) {
            selectedApps.remove(element);
            appButtons.get((index - 3)).setChecked(false);
        } else {
            selectedApps.add(element);
            appButtons.get((index - 3)).setChecked(true);
        }

        VPNPersistentData.setAppList(selectedApps);
    }
}
