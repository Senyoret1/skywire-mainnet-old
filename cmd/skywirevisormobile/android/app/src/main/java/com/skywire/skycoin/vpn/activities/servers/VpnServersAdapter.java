package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.helpers.ClickWithIndexEvent;
import com.skywire.skycoin.vpn.helpers.ListViewHolder;
import com.skywire.skycoin.vpn.network.models.VpnServer;

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

    @Override
    public ListViewHolder<ServerListButton> onCreateViewHolder(ViewGroup parent, int viewType) {
        ServerListButton view = new ServerListButton(context);
        view.setClickWithIndexEventListener(this);
        return new ListViewHolder<>(view);
    }

    @Override
    public void onBindViewHolder(ListViewHolder<ServerListButton> holder, int position) {
        String location = "-";
        if (data.get(position).geo != null) {
            location = "";
            if (data.get(position).geo.country != null) {
                location += data.get(position).geo.country;
            }

            if (data.get(position).geo.country != null && data.get(position).geo.region != null) {
                location += " / ";
            }

            if (data.get(position).geo.region != null) {
                location += data.get(position).geo.region;
            }
        }

        ((ServerListButton)holder.itemView).setIndex(position);
        ((ServerListButton)holder.itemView).changeData(data.get(position).addr, location);
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
