package com.skywire.skycoin.vpn.activities.index;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.TopTab;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.vpn.VPNCoordinator;

public class IndexActivity extends AppCompatActivity implements IndexPageAdapter.RequestTabListener {
    private ViewPager2 pager;
    private TabLayout tabs;
    private TabLayoutMediator tabLayoutMediator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        pager = findViewById(R.id.pager);
        tabs = findViewById(R.id.tabs);

        IndexPageAdapter adapter = new IndexPageAdapter(this);
        adapter.setRequestTabListener(this);
        pager.setAdapter(adapter);

        tabLayoutMediator = new TabLayoutMediator(tabs, pager, (tab, position) -> {
            if (position == 0) {
                tab.setCustomView(new TopTab(this, R.string.tmp_status_page_title));
            } else if (position == 1) {
                tab.setCustomView(new TopTab(this, R.string.tmp_select_server_title));
            } else {
                tab.setCustomView(new TopTab(this, R.string.tmp_options_title));
            }

            if (position != 0) {
                tab.getCustomView().setAlpha(0.4f);
            }
        });
        tabLayoutMediator.attach();

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getCustomView().setAlpha(1f);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getCustomView().setAlpha(0.4f);
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        tabLayoutMediator.detach();
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() != 0) {
            pager.setCurrentItem(0);
        } else {
            super.onBackPressed();

            if (VPNCoordinator.getInstance().isServiceRunning()) {
                HelperFunctions.showToast(getString(R.string.general_service_running_notification), false);
            }
        }
    }

    @Override
    public void onOpenStatusRequested() {
        pager.setCurrentItem(0);
    }

    @Override
    public void onOpenServerListRequested() {
        pager.setCurrentItem(1);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        if (request == VPNCoordinator.VPN_PREPARATION_REQUEST_CODE) {
            VPNCoordinator.getInstance().onActivityResult(request, result, data);
        }
    }
}
