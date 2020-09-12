package com.skywire.skycoin.vpn.helpers;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class ListViewHolder<T extends View> extends RecyclerView.ViewHolder {
    T view;

    public ListViewHolder(T v) {
        super(v);
        view = v;
    }

    public T getButtonView() {
        return view;
    }
}
