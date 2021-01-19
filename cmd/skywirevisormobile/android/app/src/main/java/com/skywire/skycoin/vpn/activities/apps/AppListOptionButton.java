package com.skywire.skycoin.vpn.activities.apps;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.BoxRowLayout;
import com.skywire.skycoin.vpn.extensible.ListButtonBase;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;

public class AppListOptionButton extends ListButtonBase<Void> {
    private BoxRowLayout mainLayout;
    private TextView textOption;
    private RadioButton radioSelected;

    public AppListOptionButton(Context context) {
        super(context);
    }

    @Override
    protected void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_app_list_selection_option, this, true);

        mainLayout = this.findViewById (R.id.mainLayout);
        textOption = this.findViewById (R.id.textOption);
        radioSelected = this.findViewById (R.id.radioSelected);

        radioSelected.setChecked(false);
    }

    public void setBoxRowType(BoxRowTypes type) {
        mainLayout.setType(type);
    }

    public void changeData(int textResource) {
        textOption.setText(textResource);
    }

    public void setChecked(boolean checked) {
        radioSelected.setChecked(checked);
    }
}
