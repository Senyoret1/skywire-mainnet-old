package com.skywire.skycoin.vpn.extensible;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public abstract class ListButtonBase<DataType> extends RelativeLayout implements View.OnClickListener {
    public ListButtonBase(Context context) {
        super(context);
        this.setOnClickListener(this);
        Initialize(context);
    }
    public ListButtonBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(this);
        Initialize(context);
    }
    public ListButtonBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setOnClickListener(this);
        Initialize(context);
    }

    protected DataType dataForEvent;
    private int index;
    private ClickWithIndexEvent<DataType> clickListener;

    abstract protected void Initialize (Context context);

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setClickWithIndexEventListener(ClickWithIndexEvent<DataType> listener) {
        clickListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (clickListener != null) {
            clickListener.onClickWithIndex(index, dataForEvent);
        }
    }
}
