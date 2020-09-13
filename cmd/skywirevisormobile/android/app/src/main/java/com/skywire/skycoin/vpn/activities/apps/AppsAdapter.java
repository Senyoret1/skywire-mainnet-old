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
    public interface AppListChangedListener {
        boolean onAppListChanged();
    }

    private Context context;
    private List<ResolveInfo> appList;
    private List<String> uninstalledApps;
    private AppListChangedListener appListChangedListener;

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

        HashSet<String> filteredApps = HelperFunctions.filterAvailableApps(selectedApps);
        if (filteredApps.size() != selectedApps.size()) {
            uninstalledApps = new ArrayList<>();

            for (String app : selectedApps) {
                if (!filteredApps.contains(app)) {
                    uninstalledApps.add(app);
                }
            }
        }

        optionTexts.put(0, R.string.tmp_select_apps_protect_all_button);
        optionTexts.put(1, R.string.tmp_select_apps_protect_selected_button);
        optionTexts.put(2, R.string.tmp_select_apps_unprotect_selected_button);
    }

    public void setAppListChangedEventListener(AppListChangedListener listener) {
        appListChangedListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 3) {
            return 0;
        }

        if (uninstalledApps == null) {
            return 1;
        }

        if (position == 3 || position == 4 + appList.size()) {
            return 2;
        }

        return 1;
    }

    @Override
    public ListViewHolder<View> onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            AppListOptionButton view = new AppListOptionButton(context);
            view.setClickWithIndexEventListener(this);
            optionButtons.add(view);

            return new ListViewHolder<>(view);
        } else if (viewType == 1) {
            AppListButton view = new AppListButton(context);
            view.setClickWithIndexEventListener(this);
            view.setEnabled(selectedOption != Globals.AppFilteringModes.PROTECT_ALL);
            appButtons.add(view);

            return new ListViewHolder<>(view);
        }

        AppListSeparator view = new AppListSeparator(context);

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
            ((AppListOptionButton)(holder.itemView)).setIndex(position);
            ((AppListOptionButton)(holder.itemView)).changeData(optionText);
            ((AppListOptionButton)(holder.itemView)).setChecked(showChecked);

            return;
        } else if (holder.getItemViewType() == 2) {
            if (position == 3) {
                ((AppListSeparator)holder.itemView).changeTitle(R.string.tmp_select_apps_installed_apps_title);
            } else {
                ((AppListSeparator)holder.itemView).changeTitle(R.string.tmp_select_apps_uninstalled_apps_title);
            }

            return;
        }

        int initialAppButtonsIndex = uninstalledApps == null ? 3 : 4;

        if (position < initialAppButtonsIndex + appList.size()) {
            String element = appList.get(position - initialAppButtonsIndex).activityInfo.packageName;
            ((AppListButton) (holder.itemView)).setIndex(position);
            ((AppListButton) (holder.itemView)).changeData(appList.get(position - initialAppButtonsIndex));
            ((AppListButton) (holder.itemView)).setChecked(selectedApps.contains(element));
        } else {
            String element = uninstalledApps.get(position - (initialAppButtonsIndex + appList.size() + 1));
            ((AppListButton) (holder.itemView)).setIndex(position);
            ((AppListButton) (holder.itemView)).changeData(element);
            ((AppListButton) (holder.itemView)).setChecked(selectedApps.contains(element));
        }
    }

    @Override
    public int getItemCount() {
        if (uninstalledApps == null) {
            return appList.size() + 3;
        } else {
            return appList.size() + 3 + 2 + uninstalledApps.size();
        }
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (appListChangedListener != null) {
            if (!appListChangedListener.onAppListChanged()) {
                return;
            }
        }

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
        String app;

        int initialAppButtonsIndex = uninstalledApps == null ? 3 : 4;
        if (index < initialAppButtonsIndex + appList.size()) {
            app = appList.get(index - initialAppButtonsIndex).activityInfo.packageName;
        } else {
            app = uninstalledApps.get(index - (3 + 2 + appList.size()));
        }

        if (selectedApps.contains(app)) {
            selectedApps.remove(app);

            for (AppListButton appButton : appButtons) {
                if (appButton.getAppPackageName().equals(app)) {
                    appButton.setChecked(false);
                }
            }
        } else {
            selectedApps.add(app);

            for (AppListButton appButton : appButtons) {
                if (appButton.getAppPackageName().equals(app)) {
                    appButton.setChecked(true);
                }
            }
        }

        VPNPersistentData.setAppList(selectedApps);
    }
}
