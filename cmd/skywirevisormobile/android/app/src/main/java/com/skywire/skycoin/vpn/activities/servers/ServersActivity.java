package com.skywire.skycoin.vpn.activities.servers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.LocalServerData;
import com.skywire.skycoin.vpn.objects.ServerRatings;
import com.skywire.skycoin.vpn.vpn.VPNServersPersistentData;

import java.util.ArrayList;
import java.util.Date;

import io.reactivex.rxjava3.disposables.Disposable;

public class ServersActivity extends AppCompatActivity implements VpnServersAdapter.VpnServerSelectedListener {
    public static String ADDRESS_DATA_PARAM = "address";

    private RecyclerView recycler;
    private ProgressBar loadingAnimation;

    private ServerLists listType = ServerLists.History;
    private VpnServersAdapter adapter;

    private Disposable serverSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_list);

        recycler = findViewById(R.id.recycler);
        loadingAnimation = findViewById(R.id.loadingAnimation);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        // This could be useful in the future.
        // recycler.setHasFixedSize(true);

        // This code retrieves the data from the server and populates the list with the recovered
        // data, but is not used right now as the server is returning empty arrays.
        // requestData()

        // Initialize the recycler.
        adapter = new VpnServersAdapter(this);
        adapter.setData(new ArrayList<>(), listType);
        adapter.setVpnSelectedEventListener(this);
        recycler.setAdapter(adapter);

        //requestLocalData();

        // Use test data, for now.
        showTestServers();
    }

    private void requestData() {
        if (serverSubscription != null) {
            serverSubscription.dispose();
        }

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

    private void requestLocalData() {
        if (serverSubscription != null) {
            serverSubscription.dispose();
        }

        recycler.setVisibility(View.GONE);
        loadingAnimation.setVisibility(View.VISIBLE);

        serverSubscription = VPNServersPersistentData.getInstance().history().subscribe(response -> {
            // TODO: check if the response is empty.

            ArrayList<VpnServerForList> list = new ArrayList<>();

            for (LocalServerData server : response) {
                VpnServerForList converted = new VpnServerForList();

                converted.countryCode = server.countryCode;
                converted.name = server.name;
                converted.customName = server.customName;
                converted.location = server.location;
                converted.pk = server.pk;
                converted.note = server.note;
                converted.personalNote = server.personalNote;
                converted.lastUsed = server.lastUsed;
                converted.inHistory = server.inHistory;
                converted.flag = server.flag;
                converted.originalLocalData = server;

                list.add(converted);
            }

            adapter.setData(list, listType);

            recycler.setVisibility(View.VISIBLE);
            loadingAnimation.setVisibility(View.GONE);
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
    public void onVpnServerSelected(VpnServerForList selectedServer) {
        if (HelperFunctions.closeActivityIfServiceRunning(this)) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(ADDRESS_DATA_PARAM, selectedServer.pk);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showTestServers() {
        ArrayList<VpnServerForList> servers = new ArrayList<>();

        VpnServerForList testServer = new VpnServerForList();
        testServer.lastUsed = new Date();
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
        servers.add(testServer);

        testServer = new VpnServerForList();
        testServer.lastUsed = new Date();
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
        servers.add(testServer);

        testServer = new VpnServerForList();
        testServer.lastUsed = new Date();
        testServer.countryCode = "de";
        testServer.name = "Test server 20";
        testServer.location = "Berlin";
        testServer.pk = "044ec47420176680816e0406250e7156465e4531f5b26057c9f6297bb0303558c7";
        testServer.congestion = 20;
        testServer.congestionRating = ServerRatings.Gold;
        testServer.latency = 123;
        testServer.latencyRating = ServerRatings.Bronze;
        testServer.hops = 7;
        servers.add(testServer);

        adapter.setData(servers, ServerLists.Public);

        recycler.setVisibility(View.VISIBLE);
        loadingAnimation.setVisibility(View.GONE);
    }
}
