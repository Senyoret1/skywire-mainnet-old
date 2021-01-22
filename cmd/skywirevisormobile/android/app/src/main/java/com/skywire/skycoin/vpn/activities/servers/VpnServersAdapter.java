package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.extensible.ClickWithIndexEvent;
import com.skywire.skycoin.vpn.extensible.ListViewHolder;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;

import java.util.List;

public class VpnServersAdapter extends RecyclerView.Adapter<ListViewHolder<ServerListButton>> implements ClickWithIndexEvent<Void> {
    public interface VpnServerSelectedListener {
        void onVpnServerSelected(VpnServerForList selectedServer);
    }

    private Context context;
    private List<VpnServerForList> data;
    private ServerLists listType = ServerLists.Public;
    private VpnServerSelectedListener vpnSelectedListener;

    public VpnServersAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<VpnServerForList> data, ServerLists listType) {
        this.data = data;
        this.listType = listType;
        this.notifyDataSetChanged();
    }

    public void setVpnSelectedEventListener(VpnServerSelectedListener listener) {
        vpnSelectedListener = listener;
    }

    @NonNull
    @Override
    public ListViewHolder<ServerListButton> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ServerListButton view = new ServerListButton(context);
        view.setClickWithIndexEventListener(this);
        return new ListViewHolder<>(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder<ServerListButton> holder, int position) {
        ((ServerListButton)holder.itemView).setIndex(position);
        ((ServerListButton)holder.itemView).changeData(data.get(position), listType);

        if (data.size() == 1) {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.SINGLE);
        } else if (position == 0) {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.TOP);
        } else if (position == getItemCount() - 1) {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.BOTTOM);
        } else {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.MIDDLE);
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (vpnSelectedListener != null) {
            vpnSelectedListener.onVpnServerSelected(this.data.get(index));
        }
    }
}
