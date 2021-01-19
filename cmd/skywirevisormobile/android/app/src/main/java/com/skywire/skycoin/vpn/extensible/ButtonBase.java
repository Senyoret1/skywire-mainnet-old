package com.skywire.skycoin.vpn.extensible;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public abstract class ButtonBase extends RelativeLayout implements View.OnClickListener {
    public ButtonBase(Context context) {
        super(context);
        this.setOnClickListener(this);
        Initialize(context, null);
    }
    public ButtonBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(this);
        Initialize(context, attrs);
    }
    public ButtonBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setOnClickListener(this);
        Initialize(context, attrs);
    }

    private ClickEvent clickListener;

    abstract protected void Initialize (Context context, AttributeSet attrs);

    public void setClickEventListener(ClickEvent listener) {
        clickListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (clickListener != null) {
            clickListener.onClick(this);
        }
    }
}
