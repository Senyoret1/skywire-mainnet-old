package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.extensible.ClickEvent;

public class SettingsButton extends RelativeLayout implements View.OnClickListener, View.OnTouchListener {
    private TextView textIcon;

    private ClickEvent clickListener;

    public SettingsButton(Context context) {
        super(context);
        Initialize(context);
    }
    public SettingsButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context);
    }
    public SettingsButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context);
    }

    protected void Initialize (Context context) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_settings_button, this, true);

        textIcon = this.findViewById (R.id.textIcon);

        this.setOnClickListener(this);
        this.setOnTouchListener(this);
    }

    public void setClickEventListener(ClickEvent listener) {
        clickListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (clickListener != null) {
            clickListener.onClick();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            textIcon.setAlpha(0.5f);
        } else {
            textIcon.setAlpha(1);
        }

        return false;
    }
}
