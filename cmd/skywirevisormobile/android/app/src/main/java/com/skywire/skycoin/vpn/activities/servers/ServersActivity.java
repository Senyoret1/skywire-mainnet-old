package com.skywire.skycoin.vpn.activities.servers;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.network.ApiClient;
import com.skywire.skycoin.vpn.network.models.GeoInfo;
import com.skywire.skycoin.vpn.network.models.VpnServer;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ServersActivity extends AppCompatActivity implements VpnServersAdapter.VpnServerSelectedListener {
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
    protected void onResume() {
        super.onResume();
        HelperFunctions.closeActivityIfServiceRunning(this);
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
        if (HelperFunctions.closeActivityIfServiceRunning(this)) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(ADDRESS_DATA_PARAM, selectedServer.addr);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private ArrayList<VpnServer> createTestServers() {
        ArrayList<VpnServer> response = new ArrayList<>();

        VpnServer testServer = new VpnServer();
        testServer.addr = "024ec47420176680816e0406250e7156465e4531f5b26057c9f6297bb0303558c7";
        testServer.geo = new GeoInfo();
        testServer.geo.country = "US";
        testServer.geo.region = "NY";
        response.add(testServer);

        testServer = new VpnServer();
        testServer.addr = "0348c941c5015a05c455ff238af2e57fb8f914c399aab604e9abb5b32b91a4c1fe";
        testServer.geo = new GeoInfo();
        testServer.geo.country = "US";
        testServer.geo.region = "CA";
        response.add(testServer);

        testServer = new VpnServer();
        testServer.addr = "031b80cd5773143a39d940dc0710b93dcccc262a85108018a7a95ab9af734f8055";
        testServer.geo = new GeoInfo();
        testServer.geo.country = "CA";
        testServer.geo.region = "ON";
        response.add(testServer);

        return response;
    }
}
