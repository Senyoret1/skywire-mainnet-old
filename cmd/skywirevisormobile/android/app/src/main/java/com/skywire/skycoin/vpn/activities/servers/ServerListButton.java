package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.BoxRowLayout;
import com.skywire.skycoin.vpn.extensible.ListButtonBase;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;

public class ServerListButton extends ListButtonBase<Void> {
    private BoxRowLayout mainLayout;
    private ImageView imageFlag;
    private TextView textTopLine;
    private TextView textLocation;
    private TextView textPk;

    public ServerListButton (Context context) {
        super(context);
    }

    @Override
    protected void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_server_list_item, this, true);

        mainLayout = this.findViewById (R.id.mainLayout);
        imageFlag = this.findViewById (R.id.imageFlag);
        textTopLine = this.findViewById (R.id.textName);
        textLocation = this.findViewById (R.id.textLocation);
        textPk = this.findViewById (R.id.textPk);

        imageFlag.setClipToOutline(true);
    }

    public void changeData(String name, String location, String pk, int verticalPosition) {
        textTopLine.setText(name);
        textLocation.setText(location);
        textPk.setText(pk);
    }

    public void setBoxRowType(BoxRowTypes type) {
        mainLayout.setType(type);
    }
}
