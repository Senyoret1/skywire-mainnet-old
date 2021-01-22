package com.skywire.skycoin.vpn.activities.start;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.LocalServerData;

public class CurrentServerButton extends LinearLayout {
    public CurrentServerButton(Context context) {
        super(context);
        Initialize(context, null);
    }
    public CurrentServerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public CurrentServerButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private LinearLayout serverContainer;
    private ImageView imageFlag;
    private TextView textTop;
    private TextView textBottom;
    private TextView textNoServer;

    private void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_current_server_button, this, true);

        serverContainer = this.findViewById (R.id.serverContainer);
        imageFlag = this.findViewById (R.id.imageFlag);
        textTop = this.findViewById (R.id.textTop);
        textBottom = this.findViewById (R.id.textBottom);
        textNoServer = this.findViewById (R.id.textNoServer);

        imageFlag.setClipToOutline(true);
    }

    public void setData (LocalServerData currentServer) {
        if (currentServer == null) {
            textNoServer.setVisibility(VISIBLE);
            serverContainer.setVisibility(GONE);

            return;
        }

        serverContainer.setVisibility(VISIBLE);
        textNoServer.setVisibility(GONE);

        textTop.setText(currentServer.name);
        textBottom.setText(currentServer.pk);
        imageFlag.setImageResource(HelperFunctions.getFlagResourceId(currentServer.countryCode));
    }
}
