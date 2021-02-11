package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.ManualServerModalWindow;
import com.skywire.skycoin.vpn.extensible.ClickWithIndexEvent;
import com.skywire.skycoin.vpn.extensible.ListViewHolder;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.LocalServerData;
import com.skywire.skycoin.vpn.vpn.VPNCoordinator;

import java.util.List;

public class VpnServersAdapter extends RecyclerView.Adapter<ListViewHolder<View>> implements ClickWithIndexEvent<Void> {
    public interface VpnServerSelectedListener {
        void onVpnServerSelected(VpnServerForList selectedServer);
        void onManualEntered(LocalServerData server);
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

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        }

        return 1;
    }

    @NonNull
    @Override
    public ListViewHolder<View> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            ServerListOptions view = new ServerListOptions(context);
            view.setClickWithIndexEventListener(this);
            return new ListViewHolder<>(view);
        }

        ServerListButton view = new ServerListButton(context);
        view.setClickWithIndexEventListener(this);
        return new ListViewHolder<>(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder<View> holder, int position) {
        if (position != 0) {
            position -= 1;

            ((ServerListButton) holder.itemView).setIndex(position);
            ((ServerListButton) holder.itemView).changeData(data.get(position), listType);

            if (data.size() == 1) {
                ((ServerListButton) holder.itemView).setBoxRowType(BoxRowTypes.SINGLE);
            } else if (position == 0) {
                ((ServerListButton) holder.itemView).setBoxRowType(BoxRowTypes.TOP);
            } else if (position == data.size() - 1) {
                ((ServerListButton) holder.itemView).setBoxRowType(BoxRowTypes.BOTTOM);
            } else {
                ((ServerListButton) holder.itemView).setBoxRowType(BoxRowTypes.MIDDLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? (data.size() + 1) : 1;
    }

    @Override
    public void onClickWithIndex(int index, Void data) {
        if (vpnSelectedListener != null) {
            if (index >= 0) {
                vpnSelectedListener.onVpnServerSelected(this.data.get(index));
            } else {
                if (index == ServerListOptions.addIndex) {
                    if (VPNCoordinator.getInstance().isServiceRunning()) {
                        HelperFunctions.showToast(context.getText(R.string.tmp_select_server_running_error).toString(), true);
                        return;
                    }

                    ManualServerModalWindow modal = new ManualServerModalWindow(context, server -> vpnSelectedListener.onManualEntered(server));
                    modal.show();
                }
            }
        }
    }
}
