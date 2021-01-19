package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.skywire.skycoin.vpn.R;

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

    private ImageView imageFlag;

    private void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_current_server_button, this, true);

        imageFlag = this.findViewById (R.id.imageFlag);
        imageFlag.setClipToOutline(true);
    }
}
