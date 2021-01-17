package com.skywire.skycoin.vpn.activities.servers;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.ServerRatings;
import com.skywire.skycoin.vpn.objects.VpnServer;

import java.util.ArrayList;

import io.reactivex.rxjava3.disposables.Disposable;

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
        /*
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
        */
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
        resultIntent.putExtra(ADDRESS_DATA_PARAM, selectedServer.pk);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private ArrayList<VpnServer> createTestServers() {
        ArrayList<VpnServer> response = new ArrayList<>();

        VpnServer testServer = new VpnServer();
        testServer.countryCode = "au";
        testServer.name = "Server name";
        testServer.location = "Melbourne";
        testServer.pk = "024ec47420176680816e0406250e7156465e4531f5b26057c9f6297bb0303558c7";
        testServer.congestion = 20;
        testServer.congestionRating = ServerRatings.Gold;
        testServer.latency = 123;
        testServer.latencyRating = ServerRatings.Gold;
        testServer.hops = 3;
        testServer.note = "Note";
        response.add(testServer);

        testServer = new VpnServer();
        testServer.countryCode = "br";
        testServer.name = "Test server 14";
        testServer.location = "Rio de Janeiro";
        testServer.pk = "034ec47420176680816e0406250e7156465e4531f5b26057c9f6297bb0303558c7";
        testServer.congestion = 20;
        testServer.congestionRating = ServerRatings.Silver;
        testServer.latency = 12345;
        testServer.latencyRating = ServerRatings.Gold;
        testServer.hops = 3;
        testServer.note = "Note";
        response.add(testServer);

        testServer = new VpnServer();
        testServer.countryCode = "de";
        testServer.name = "Test server 20";
        testServer.location = "Berlin";
        testServer.pk = "044ec47420176680816e0406250e7156465e4531f5b26057c9f6297bb0303558c7";
        testServer.congestion = 20;
        testServer.congestionRating = ServerRatings.Gold;
        testServer.latency = 123;
        testServer.latencyRating = ServerRatings.Bronze;
        testServer.hops = 7;
        response.add(testServer);

        return response;
    }
}
