package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.skywire.skycoin.vpn.R;

public class TopBar extends LinearLayout {
    public TopBar(Context context) {
        super(context);
        Initialize(context);
    }
    public TopBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context);
    }
    public TopBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context);
    }

    private void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.control_top_bar, this, true);
    }
}
