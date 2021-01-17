package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.extensible.ClickWithIndexEvent;
import com.skywire.skycoin.vpn.extensible.ListViewHolder;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;
import com.skywire.skycoin.vpn.objects.VpnServer;

import java.util.List;

public class VpnServersAdapter extends RecyclerView.Adapter<ListViewHolder<ServerListButton>> implements ClickWithIndexEvent<Void> {
    public interface VpnServerSelectedListener {
        void onVpnServerSelected(VpnServer selectedServer);
    }

    private Context context;
    private List<VpnServer> data;
    private VpnServerSelectedListener vpnSelectedListener;

    public VpnServersAdapter(Context context, List<VpnServer> data) {
        this.context = context;
        this.data = data;
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
        ((ServerListButton)holder.itemView).changeData(data.get(position), holder.getLayoutPosition());

        if (position == 0) {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.TOP);
        } else if (position == getItemCount() - 1) {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.BOTTOM);
        } else {
            ((ServerListButton)holder.itemView).setBoxRowType(BoxRowTypes.MIDDLE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (vpnSelectedListener != null) {
            vpnSelectedListener.onVpnServerSelected(this.data.get(index));
        }
    }
}
