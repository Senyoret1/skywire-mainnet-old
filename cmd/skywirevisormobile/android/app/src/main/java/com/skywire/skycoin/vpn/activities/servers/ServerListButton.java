package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.ListButtonBase;

public class ServerListButton extends ListButtonBase<Void> {
    private TextView textTopLine;
    private TextView textBottomLine;

    public ServerListButton (Context context) {
        super(context);
    }

    @Override
    protected void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_server_list_item, this, true);

        textTopLine = this.findViewById (R.id.textTopLine);
        textBottomLine = this.findViewById (R.id.textBottomLine);
    }

    public void changeData(String topText, String bottomText) {
        textTopLine.setText(topText);
        textBottomLine.setText(bottomText);
    }
}
