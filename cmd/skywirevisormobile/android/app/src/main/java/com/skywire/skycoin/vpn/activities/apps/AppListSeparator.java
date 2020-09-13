package com.skywire.skycoin.vpn.activities.apps;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;

public class AppListSeparator extends LinearLayout {
    private TextView textTitle;

    public AppListSeparator(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_app_list_separator, this, true);

        textTitle = this.findViewById (R.id.textTitle);
    }

    public void changeTitle(int title) {
        textTitle.setText(title);
    }
}
