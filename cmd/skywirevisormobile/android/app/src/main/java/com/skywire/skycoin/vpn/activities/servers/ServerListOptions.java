package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.extensible.ClickEvent;
import com.skywire.skycoin.vpn.extensible.ClickWithIndexEvent;

public class ServerListOptions extends FrameLayout implements ClickEvent {
    public static final int filterIndex = -1;
    public static final int addIndex = -2;

    public ServerListOptions(Context context) {
        super(context);
        Initialize(context, null);
    }
    public ServerListOptions(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public ServerListOptions(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private ServerListOptionButton buttonFilter;
    private ServerListOptionButton buttonAdd;

    private ClickWithIndexEvent<Void> clickListener;

    protected void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.view_server_list_options, this, true);

        buttonFilter = this.findViewById (R.id.buttonFilter);
        buttonAdd = this.findViewById (R.id.buttonAdd);

        buttonFilter.setClickEventListener(this);
        buttonAdd.setClickEventListener(this);

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootView.setLayoutParams(params);
    }

    public void setClickWithIndexEventListener(ClickWithIndexEvent<Void> listener) {
        clickListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (clickListener != null) {
            if (view.getId() == R.id.buttonAdd) {
                clickListener.onClickWithIndex(addIndex, null);
            }
        }
    }
}
