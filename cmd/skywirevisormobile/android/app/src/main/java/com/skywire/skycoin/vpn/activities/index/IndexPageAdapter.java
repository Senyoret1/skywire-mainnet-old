package com.skywire.skycoin.vpn.activities.index;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.skywire.skycoin.vpn.activities.servers.ServersActivity;
import com.skywire.skycoin.vpn.activities.settings.SettingsActivity;
import com.skywire.skycoin.vpn.activities.start.StartActivity;

public class IndexPageAdapter extends FragmentStateAdapter {
    public interface RequestTabListener {
        void onOpenStatusRequested();
        void onOpenServerListRequested();
    }

    private RequestTabListener requestTabListener;

    private RequestTabListener internalEventListener = new RequestTabListener() {
        @Override
        public void onOpenStatusRequested() {
            if (requestTabListener != null) {
                requestTabListener.onOpenStatusRequested();
            }
        }

        @Override
        public void onOpenServerListRequested() {
            if (requestTabListener != null) {
                requestTabListener.onOpenServerListRequested();
            }
        }
    };

    public IndexPageAdapter(AppCompatActivity activity) {
        super(activity);
    }

    public void setRequestTabListener(RequestTabListener listener) {
        requestTabListener = listener;
    }

    @Override
    public Fragment createFragment(int position) {
        Fragment response;

        if (position == 0) {
            response = new StartActivity();
            ((StartActivity)response).setRequestTabListener(internalEventListener);
        } else if (position == 1) {
            response = new ServersActivity();
            ((ServersActivity)response).setRequestTabListener(internalEventListener);
        } else {
            response = new SettingsActivity();
        }

        return response;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
