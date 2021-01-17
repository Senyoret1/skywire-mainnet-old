package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.skywire.skycoin.vpn.App;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.BoxRowLayout;
import com.skywire.skycoin.vpn.controls.SettingsButton;
import com.skywire.skycoin.vpn.extensible.ListButtonBase;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.ServerRatings;
import com.skywire.skycoin.vpn.objects.VpnServer;

import skywiremob.Skywiremob;

public class ServerListButton extends ListButtonBase<Void> {
    private BoxRowLayout mainLayout;
    private ImageView imageFlag;
    private TextView textTopLine;
    private TextView textLocation;
    private TextView textLatency;
    private TextView textCongestion;
    private TextView textHops;
    private TextView textLatencyRating;
    private TextView textCongestionRating;
    private TextView textNote;
    private LinearLayout noteArea;
    private SettingsButton buttonSettings;

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
        textLatency = this.findViewById (R.id.textLatency);
        textCongestion = this.findViewById (R.id.textCongestion);
        textHops = this.findViewById (R.id.textHops);
        textLatencyRating = this.findViewById (R.id.textLatencyRating);
        textCongestionRating = this.findViewById (R.id.textCongestionRating);
        textNote = this.findViewById (R.id.textNote);
        noteArea = this.findViewById (R.id.noteArea);
        buttonSettings = this.findViewById (R.id.buttonSettings);

        imageFlag.setClipToOutline(true);

        buttonSettings.setClickEventListener(() -> Skywiremob.printString("Settings"));
    }

    public void changeData(VpnServer serverData, int verticalPosition) {
        textTopLine.setText(serverData.name);
        textLatency.setText(HelperFunctions.getLatencyValue(serverData.latency, getContext()));
        textCongestion.setText(HelperFunctions.zeroDecimalsFormatter.format(serverData.congestion) + "%");
        textHops.setText(serverData.hops + "");

        String pk = serverData.pk;
        if (pk.length() > 5) {
            pk = pk.substring(0, 5);
        }
        textLocation.setText("(" + pk + ") " + serverData.location);

        if (serverData.note != null && serverData.note.trim() != "") {
            noteArea.setVisibility(VISIBLE);
            textNote.setText(serverData.note);
        } else {
            noteArea.setVisibility(GONE);
        }

        textLatencyRating.setText(ServerRatings.getTextForRating(serverData.latencyRating));
        textLatencyRating.setTextColor(getRatingColor(serverData.latencyRating));
        textCongestionRating.setText(ServerRatings.getTextForRating(serverData.congestionRating));
        textCongestionRating.setTextColor(getRatingColor(serverData.congestionRating));

        if (serverData.congestion < 60) {
            textCongestion.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        } else if (serverData.congestion < 90) {
            textCongestion.setTextColor(ContextCompat.getColor(getContext(), R.color.yellow));
        } else {
            textCongestion.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
        }

        if (serverData.latency < 200) {
            textLatency.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        } else if (serverData.latency < 350) {
            textLatency.setTextColor(ContextCompat.getColor(getContext(), R.color.yellow));
        } else {
            textLatency.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
        }

        if (serverData.hops < 5) {
            textHops.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        } else if (serverData.hops < 9) {
            textHops.setTextColor(ContextCompat.getColor(getContext(), R.color.yellow));
        } else {
            textHops.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
        }

        if (serverData.countryCode.toLowerCase() != "do") {
            int flagResourceId = getResources().getIdentifier(
                    serverData.countryCode,
                    "drawable",
                    App.getContext().getPackageName()
            );

            if (flagResourceId != 0) {
                imageFlag.setImageResource(flagResourceId);
            } else {
                imageFlag.setImageResource(R.drawable.zz);
            }
        } else {
            imageFlag.setImageResource(R.drawable.do_flag);
        }
    }

    public void setBoxRowType(BoxRowTypes type) {
        mainLayout.setType(type);
    }

    private int getRatingColor(ServerRatings rating) {
        int colorId = R.color.bronze;

        if (rating == ServerRatings.Gold) {
            colorId = R.color.gold;
        } else if (rating == ServerRatings.Silver) {
            colorId = R.color.silver;
        }

        return ContextCompat.getColor(getContext(), colorId);
    }
}
