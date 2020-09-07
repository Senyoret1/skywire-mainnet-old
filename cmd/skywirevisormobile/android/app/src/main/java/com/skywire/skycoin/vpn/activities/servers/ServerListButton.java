package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;

public class ServerListButton extends RelativeLayout implements View.OnClickListener {
    public interface ClickWithIndexEvent {
        void onClickWithIndex(int index);
    }

    public ServerListButton (Context context) {
        super(context);
        Initialize(context);
    }
    public ServerListButton (Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context);
    }
    public ServerListButton (Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context);
    }

    private TextView textTopLine;
    private TextView textBottomLine;

    private int index;
    private ClickWithIndexEvent clickListener;

    private void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_server_list_item, this, true);

        textTopLine = this.findViewById (R.id.textTopLine);
        textBottomLine = this.findViewById (R.id.textBottomLine);

        this.setOnClickListener(this);
    }

    public void changeTexts(String topText, String bottomText) {
        textTopLine.setText(topText);
        textBottomLine.setText(bottomText);
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setClickWithIndexEventListener(ClickWithIndexEvent listener) {
        clickListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (clickListener != null) {
            clickListener.onClickWithIndex(index);
        }
    }
}
