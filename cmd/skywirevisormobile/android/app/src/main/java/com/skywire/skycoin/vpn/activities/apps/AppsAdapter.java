package com.skywire.skycoin.vpn.activities.apps;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;
import com.skywire.skycoin.vpn.extensible.ClickWithIndexEvent;
import com.skywire.skycoin.vpn.helpers.Globals;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.extensible.ListViewHolder;
import com.skywire.skycoin.vpn.vpn.VPNGeneralPersistentData;

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

    private int[] optionTexts = new int[3];
    private int[] optionDescriptions = new int[3];
    private ArrayList<AppListOptionButton> optionButtons = new ArrayList<>();
    private ArrayList<AppListButton> appButtons = new ArrayList<>();

    public AppsAdapter(Context context) {
        this.context = context;

        selectedApps = VPNGeneralPersistentData.getAppList(new HashSet<>());
        changeSelectedOption(VPNGeneralPersistentData.getAppsSelectionMode());

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

        optionTexts[0] =  R.string.tmp_select_apps_protect_all_button;
        optionTexts[1] =  R.string.tmp_select_apps_protect_selected_button;
        optionTexts[2] =  R.string.tmp_select_apps_unprotect_selected_button;

        optionDescriptions[0] =  R.string.tmp_select_apps_protect_all_button_desc;
        optionDescriptions[1] =  R.string.tmp_select_apps_protect_selected_button_desc;
        optionDescriptions[2] =  R.string.tmp_select_apps_unprotect_selected_button_desc;
    }

    public void setAppListChangedEventListener(AppListChangedListener listener) {
        appListChangedListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == 4 || position == 5 + appList.size()) {
            return 2;
        }

        if (position < 4) {
            return 0;
        }

        return 1;
    }

    @NonNull
    @Override
    public ListViewHolder<View> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(@NonNull ListViewHolder<View> holder, int position) {
        if (holder.getItemViewType() == 0) {
            boolean showChecked = false;
            if (position == 1 && selectedOption == Globals.AppFilteringModes.PROTECT_ALL) { showChecked = true; }
            if (position == 2 && selectedOption == Globals.AppFilteringModes.PROTECT_SELECTED) { showChecked = true; }
            if (position == 3 && selectedOption == Globals.AppFilteringModes.IGNORE_SELECTED) { showChecked = true; }

            ((AppListOptionButton)(holder.itemView)).setIndex(position);
            ((AppListOptionButton)(holder.itemView)).changeData(optionTexts[position - 1], optionDescriptions[position - 1]);
            ((AppListOptionButton)(holder.itemView)).setChecked(showChecked);

            if (position == 1) {
                ((AppListOptionButton)holder.itemView).setBoxRowType(BoxRowTypes.TOP);
            } else if (position == 2) {
                ((AppListOptionButton)holder.itemView).setBoxRowType(BoxRowTypes.MIDDLE);
            } else {
                ((AppListOptionButton)holder.itemView).setBoxRowType(BoxRowTypes.BOTTOM);
            }

            return;
        } else if (holder.getItemViewType() == 2) {
            if (position == 0) {
                ((AppListSeparator)holder.itemView).changeTitle(R.string.tmp_select_apps_mode_title);
            } else if (position == 4) {
                if (this.uninstalledApps != null) {
                    ((AppListSeparator) holder.itemView).changeTitle(R.string.tmp_select_apps_installed_apps_title);
                } else {
                    ((AppListSeparator) holder.itemView).changeTitle(R.string.tmp_select_apps_apps_title);
                }
            } else {
                ((AppListSeparator)holder.itemView).changeTitle(R.string.tmp_select_apps_uninstalled_apps_title);
            }

            return;
        }

        int initialInstalledAppsButtonIndex = 5;
        if (position < initialInstalledAppsButtonIndex + appList.size()) {
            String element = appList.get(position - initialInstalledAppsButtonIndex).activityInfo.packageName;
            ((AppListButton) (holder.itemView)).setIndex(position);
            ((AppListButton) (holder.itemView)).changeData(appList.get(position - initialInstalledAppsButtonIndex));
            ((AppListButton) (holder.itemView)).setChecked(selectedApps.contains(element));

            if (appList.size() == 1) {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.SINGLE);
            } else if (position == initialInstalledAppsButtonIndex) {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.TOP);
            } else if (position == initialInstalledAppsButtonIndex + appList.size() - 1) {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.BOTTOM);
            } else {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.MIDDLE);
            }
        } else {
            int initialUninstalledAppsButtonIndex = initialInstalledAppsButtonIndex + appList.size() + 1;

            String element = uninstalledApps.get(position - initialUninstalledAppsButtonIndex);
            ((AppListButton) (holder.itemView)).setIndex(position);
            ((AppListButton) (holder.itemView)).changeData(element);
            ((AppListButton) (holder.itemView)).setChecked(selectedApps.contains(element));

            if (uninstalledApps.size() == 1) {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.SINGLE);
            } else if (position == initialUninstalledAppsButtonIndex) {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.TOP);
            } else if (position == initialUninstalledAppsButtonIndex + uninstalledApps.size() - 1) {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.BOTTOM);
            } else {
                ((AppListButton)holder.itemView).setBoxRowType(BoxRowTypes.MIDDLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        int result = 3 + 2 + appList.size();

        if (uninstalledApps != null) {
            result += 1 + uninstalledApps.size();
        }

        return result;
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (appListChangedListener != null) {
            if (!appListChangedListener.onAppListChanged()) {
                return;
            }
        }

        if (index < 4) {
            if (index == 1) {
                changeSelectedOption(Globals.AppFilteringModes.PROTECT_ALL);
            } else if (index == 2) {
                changeSelectedOption(Globals.AppFilteringModes.PROTECT_SELECTED);
            } else if (index == 3) {
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
            VPNGeneralPersistentData.setAppsSelectionMode(selectedOption);

            for (AppListOptionButton optionButton : optionButtons) {
                optionButton.setChecked(
                    (optionButton.getIndex() == 1 && selectedOption == Globals.AppFilteringModes.PROTECT_ALL) ||
                    (optionButton.getIndex() == 2 && selectedOption == Globals.AppFilteringModes.PROTECT_SELECTED) ||
                    (optionButton.getIndex() == 3 && selectedOption == Globals.AppFilteringModes.IGNORE_SELECTED)
                );
            }
        }
    }

    private void processAppClicked(int index) {
        String app;

        int initialAppButtonsIndex = 5;
        if (index < initialAppButtonsIndex + appList.size()) {
            app = appList.get(index - initialAppButtonsIndex).activityInfo.packageName;
        } else {
            app = uninstalledApps.get(index - (initialAppButtonsIndex + appList.size() + 1));
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

        VPNGeneralPersistentData.setAppList(selectedApps);
    }
}
