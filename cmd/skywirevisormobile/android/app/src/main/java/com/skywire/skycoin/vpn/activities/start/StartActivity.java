package com.skywire.skycoin.vpn.activities.start;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.vpn.VPNServersPersistentData;

import io.reactivex.rxjava3.disposables.Disposable;

public class StartActivity extends AppCompatActivity {
    private CurrentServerButton viewCurrentServerButton;
    private StartButton startButton;

    private Disposable currentServerSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        viewCurrentServerButton = findViewById(R.id.viewCurrentServerButton);
        startButton = findViewById(R.id.startButton);
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentServerSubscription = VPNServersPersistentData.getInstance().getCurrentServerObservable().subscribe(currentServer -> {
            viewCurrentServerButton.setData(currentServer);
        });

        startButton.startAnimation();
    }

    @Override
    protected void onStop() {
        super.onStop();

        currentServerSubscription.dispose();

        startButton.stopAnimation();
    }
}
