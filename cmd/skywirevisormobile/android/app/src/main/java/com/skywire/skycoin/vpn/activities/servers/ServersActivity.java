package com.skywire.skycoin.vpn.activities.servers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.network.ApiClient;
import com.skywire.skycoin.vpn.network.models.GeoInfo;
import com.skywire.skycoin.vpn.network.models.VpnServer;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ServersActivity extends Activity implements VpnServersAdapter.VpnServerSelectedListener {
    public static String ADDRESS_DATA_PARAM = "address";

    private RecyclerView recycler;

    private Disposable serverSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        recycler = findViewById(R.id.recycler);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        // This could be useful in the future.
        // recycler.setHasFixedSize(true);

        // This code retrieves the data from the server and populates the list with the recovered
        // data, but is not used right now as the server is returning empty arrays.
        // requestData()

        // Use test data, for now.
        VpnServersAdapter adapter = new VpnServersAdapter(this, createTestServers());
        adapter.setVpnSelectedEventListener(this);
        recycler.setAdapter(adapter);
    }

    private void requestData() {
        serverSubscription = ApiClient.getVpnServers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(response -> {
                VpnServersAdapter adapter = new VpnServersAdapter(this, response.body());
                adapter.setVpnSelectedEventListener(this);
                recycler.setAdapter(adapter);
            }, err -> {
                this.requestData();
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSubscription != null) {
            serverSubscription.dispose();
        }
    }

    @Override
    public void onVpnServerSelected(VpnServer selectedServer) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(ADDRESS_DATA_PARAM, selectedServer.addr);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private ArrayList<VpnServer> createTestServers() {
        ArrayList<VpnServer> response = new ArrayList<>();

        VpnServer testServer = new VpnServer();
        testServer.addr = "Test address 1";
        testServer.geo = new GeoInfo();
        testServer.geo.country = "US";
        testServer.geo.region = "NY";
        response.add(testServer);

        testServer = new VpnServer();
        testServer.addr = "Test address 2";
        testServer.geo = new GeoInfo();
        testServer.geo.country = "US";
        testServer.geo.region = "CA";
        response.add(testServer);

        testServer = new VpnServer();
        testServer.addr = "Test address 3";
        testServer.geo = new GeoInfo();
        testServer.geo.country = "CA";
        testServer.geo.region = "ON";
        response.add(testServer);

        return response;
    }
}
