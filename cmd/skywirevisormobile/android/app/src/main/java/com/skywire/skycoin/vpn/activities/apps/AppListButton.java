package com.skywire.skycoin.vpn.activities.apps;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.BoxRowLayout;
import com.skywire.skycoin.vpn.extensible.ListButtonBase;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;

public class AppListButton extends ListButtonBase<Void> {
    private BoxRowLayout mainLayout;
    private LinearLayout internalLayout;
    private ImageView imageIcon;
    private FrameLayout layoutSeparator;
    private TextView textAppName;
    private CheckBox checkSelected;

    private String appPackageName;

    public AppListButton(Context context) {
        super(context);
    }

    @Override
    protected void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_app_list_item, this, true);

        mainLayout = this.findViewById (R.id.mainLayout);
        internalLayout = this.findViewById (R.id.internalLayout);
        imageIcon = this.findViewById (R.id.imageIcon);
        layoutSeparator = this.findViewById (R.id.layoutSeparator);
        textAppName = this.findViewById (R.id.textAppName);
        checkSelected = this.findViewById (R.id.checkSelected);
    }

    public void changeData(ResolveInfo appData) {
        appPackageName = appData.activityInfo.packageName;
        imageIcon.setImageDrawable(appData.activityInfo.loadIcon(this.getContext().getPackageManager()));
        textAppName.setText(appData.activityInfo.loadLabel(this.getContext().getPackageManager()));
        imageIcon.setVisibility(VISIBLE);
        layoutSeparator.setVisibility(VISIBLE);
    }

    public void changeData(String appPackageName) {
        this.appPackageName = appPackageName;
        textAppName.setText(appPackageName);
        imageIcon.setVisibility(GONE);
        layoutSeparator.setVisibility(GONE);
    }

    public void setBoxRowType(BoxRowTypes type) {
        mainLayout.setType(type);
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public void setChecked(boolean checked) {
        checkSelected.setChecked(checked);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            internalLayout.setAlpha(1f);
        } else {
            internalLayout.setAlpha(0.5f);
        }
    }
}
