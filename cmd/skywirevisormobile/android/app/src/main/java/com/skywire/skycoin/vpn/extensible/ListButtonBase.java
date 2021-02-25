package com.skywire.skycoin.vpn.extensible;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.skywire.skycoin.vpn.helpers.ClickTimeManagement;
import com.skywire.skycoin.vpn.helpers.Globals;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class ListButtonBase<DataType> extends RelativeLayout implements View.OnClickListener {
    public ListButtonBase(Context context) {
        super(context);
        this.setOnClickListener(this);
        Initialize(context, null);
    }
    public ListButtonBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(this);
        Initialize(context, attrs);
    }
    public ListButtonBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setOnClickListener(this);
        Initialize(context, attrs);
    }

    protected DataType dataForEvent;
    private int index;
    private ClickWithIndexEvent<DataType> clickListener;
    private ClickTimeManagement buttonTimeManager = new ClickTimeManagement();

    abstract protected void Initialize (Context context, AttributeSet attrs);

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
        if (clickListener != null && buttonTimeManager.canClick()) {
            buttonTimeManager.informClickMade();
            Observable.just(1).delay(Globals.CLICK_DELAY_MS, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> clickListener.onClickWithIndex(index, dataForEvent));
        }
    }
}
