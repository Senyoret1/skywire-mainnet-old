package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.network.models.VpnServer;

import java.util.List;

public class VpnServersAdapter extends RecyclerView.Adapter<VpnServersAdapter.ListViewHolder> implements ServerListButton.ClickWithIndexEvent {
    public interface VpnServerSelectedListener {
        void onVpnServerSelected(VpnServer selectedServer);
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {
        ServerListButton view;

        public ListViewHolder(ServerListButton v) {
            super(v);
            view = v;
        }

        public ServerListButton getButtonView() {
            return view;
        }
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
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ServerListButton view = new ServerListButton(context);
        view.setClickWithIndexEventListener(this);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, int position) {
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

        holder.getButtonView().changeTexts(data.get(position).addr, location);
        holder.getButtonView().setIndex(position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onClickWithIndex(int index) {
        if (vpnSelectedListener != null) {
            vpnSelectedListener.onVpnServerSelected(data.get(index));
        }
    }
}
