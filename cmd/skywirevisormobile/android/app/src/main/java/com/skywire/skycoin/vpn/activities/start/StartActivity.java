package com.skywire.skycoin.vpn.activities.start;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.activities.index.IndexPageAdapter;
import com.skywire.skycoin.vpn.activities.servers.ServerLists;
import com.skywire.skycoin.vpn.activities.servers.ServersActivity;
import com.skywire.skycoin.vpn.extensible.ClickEvent;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.LocalServerData;
import com.skywire.skycoin.vpn.vpn.VPNServersPersistentData;

import io.reactivex.rxjava3.disposables.Disposable;
import skywiremob.Skywiremob;

public class StartActivity extends Fragment implements ClickEvent {
    private MapBackground background;
    private CurrentServerButton viewCurrentServerButton;
    private StartButton startButton;

    private IndexPageAdapter.RequestTabListener requestTabListener;
    private Disposable currentServerSubscription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.activity_start, container, true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        background = view.findViewById(R.id.background);
        viewCurrentServerButton = view.findViewById(R.id.viewCurrentServerButton);
        startButton = view.findViewById(R.id.startButton);

        viewCurrentServerButton.setClickEventListener(this);
        startButton.setClickEventListener(this);
    }

    public void setRequestTabListener(IndexPageAdapter.RequestTabListener listener) {
        requestTabListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();

        currentServerSubscription = VPNServersPersistentData.getInstance().getCurrentServerObservable().subscribe(currentServer -> {
            viewCurrentServerButton.setData(currentServer);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        background.resumeAnimation();
        startButton.startAnimation();
    }

    @Override
    public void onPause() {
        super.onPause();

        background.pauseAnimation();
        startButton.stopAnimation();
    }

    @Override
    public void onStop() {
        super.onStop();

        currentServerSubscription.dispose();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        background.cancelAnimation();
    }

    @Override
    public void onClick(View view) {
        LocalServerData currentServer = VPNServersPersistentData.getInstance().getCurrentServer();
        if (currentServer != null) {
            if (view.getId() == R.id.viewCurrentServerButton) {
                HelperFunctions.showServerOptions(getContext(), ServersActivity.convertLocalServerData(currentServer), ServerLists.History);
            } else {
                Skywiremob.printString("Press");
            }
        } else {
            if (requestTabListener != null) {
                requestTabListener.onOpenServerListRequested();
            }
        }
    }
}
