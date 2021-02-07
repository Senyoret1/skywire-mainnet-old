package com.skywire.skycoin.vpn.activities.start.connected;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.ConfirmationModalWindow;
import com.skywire.skycoin.vpn.extensible.ClickEvent;
import com.skywire.skycoin.vpn.network.ApiClient;
import com.skywire.skycoin.vpn.vpn.VPNCoordinator;
import com.skywire.skycoin.vpn.vpn.VPNGeneralPersistentData;
import com.skywire.skycoin.vpn.vpn.VPNStates;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class StartViewConnected extends FrameLayout implements ClickEvent, Closeable {
    public StartViewConnected(Context context) {
        super(context);
        Initialize(context, null);
    }
    public StartViewConnected(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public StartViewConnected(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private final int retryDelay = 20000;

    private TextView textState;
    private TextView textStateDescription;
    private TextView textLastError;
    private TextView textWaitingIp;
    private TextView textWaitingCountry;
    private TextView textIp;
    private TextView textCountry;
    private TextView textStartedByTheSystem;
    private ImageView imageStateLine;
    private LinearLayout ipContainer;
    private LinearLayout countryContainer;
    private ProgressBar progressIp;
    private ProgressBar progressCountry;
    private StopButton buttonStop;

    private String previousIp;
    private String currentIp;
    private String previousCountry;

    private Disposable serviceSubscription;
    private Disposable ipSubscription;

    protected void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_start_connected, this, true);

        textState = findViewById(R.id.textState);
        textStateDescription = findViewById(R.id.textStateDescription);
        textLastError = findViewById(R.id.textLastError);
        textWaitingIp = findViewById(R.id.textWaitingIp);
        textWaitingCountry = findViewById(R.id.textWaitingCountry);
        textIp = findViewById(R.id.textIp);
        textCountry = findViewById(R.id.textCountry);
        textStartedByTheSystem = findViewById(R.id.textStartedByTheSystem);
        imageStateLine = findViewById(R.id.imageStateLine);
        ipContainer = findViewById(R.id.ipContainer);
        countryContainer = findViewById(R.id.countryContainer);
        progressIp = findViewById(R.id.progressIp);
        progressCountry = findViewById(R.id.progressCountry);
        buttonStop = findViewById(R.id.buttonStop);

        textLastError.setVisibility(GONE);
        textStartedByTheSystem.setVisibility(GONE);
        ipContainer.setVisibility(GONE);
        countryContainer.setVisibility(GONE);

        if (!VPNGeneralPersistentData.getShowIpActivated()) {
            textWaitingIp.setText(R.string.tmp_status_connected_ip_option_disabled);
            textWaitingCountry.setText(R.string.tmp_status_connected_ip_option_disabled);
        }

        buttonStop.setClickEventListener(this);

        serviceSubscription = VPNCoordinator.getInstance().getEventsObservable().subscribe(
            state -> {
                int mainText = VPNStates.getTitleForState(state.state);
                if (mainText != -1) {
                    textState.setText(mainText);
                } else {
                    textState.setText("---");
                }

                imageStateLine.setBackgroundResource(VPNStates.getColorForStateTitle(mainText));

                int description = VPNStates.getDescriptionForState(state.state);
                if (description != -1) {
                    textStateDescription.setText(description);
                } else {
                    textStateDescription.setText("---");
                }

                buttonStop.setEnabled(true);

                if (state.startedByTheSystem) {
                    buttonStop.setEnabled(false);
                    textStartedByTheSystem.setVisibility(View.VISIBLE);
                } else {
                    textStartedByTheSystem.setVisibility(View.GONE);
                }

                if (state.stopRequested) {
                    buttonStop.setEnabled(false);
                    buttonStop.setBusyState(true);
                } else {
                    buttonStop.setBusyState(false);
                }

                if (state.state != VPNStates.CONNECTED) {
                    String lastError = VPNGeneralPersistentData.getLastError(null);
                    if (lastError != null) {
                        String start = getContext().getString(R.string.tmp_status_page_last_error);
                        textLastError.setText(start + " " + lastError);
                        textLastError.setVisibility(VISIBLE);
                    } else {
                        textLastError.setVisibility(GONE);
                    }
                } else {
                    VPNGeneralPersistentData.removeLastError();
                    textLastError.setVisibility(GONE);
                }

                if (VPNGeneralPersistentData.getShowIpActivated()) {
                    if (state.state == VPNStates.CONNECTED) {
                        if (ipContainer.getVisibility() == TextView.GONE) {
                            ipContainer.setVisibility(VISIBLE);
                            countryContainer.setVisibility(VISIBLE);
                            textWaitingIp.setVisibility(GONE);
                            textWaitingCountry.setVisibility(GONE);

                            textIp.setText("---");
                            textCountry.setText("---");

                            getIp(0);
                        }
                    } else {
                        if (ipContainer.getVisibility() == TextView.VISIBLE) {
                            ipContainer.setVisibility(GONE);
                            countryContainer.setVisibility(GONE);
                            textWaitingIp.setVisibility(VISIBLE);
                            textWaitingCountry.setVisibility(VISIBLE);

                            cancelIpCheck();
                        }
                    }
                }
            }
        );
    }

    private void getIp(int delayMs) {
        if (!VPNGeneralPersistentData.getShowIpActivated()) {
            return;
        }

        if (ipSubscription != null) {
            ipSubscription.dispose();
        }

        progressIp.setVisibility(VISIBLE);
        progressCountry.setVisibility(VISIBLE);

        this.ipSubscription = Observable.just(0).delay(delayMs, TimeUnit.MILLISECONDS).flatMap(v -> ApiClient.getCurrentIp())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(response -> {
                if (response.body() != null) {
                    progressIp.setVisibility(GONE);

                    currentIp = response.body().ip;
                    textIp.setText(currentIp);

                    if (currentIp.equals(previousIp) && previousCountry != null) {
                        textCountry.setText(previousCountry);
                        progressCountry.setVisibility(GONE);
                    } else {
                        getIpCountry(0);
                    }

                    previousIp = currentIp;
                } else {
                    getIp(retryDelay);
                }
            }, err -> {
                getIp(retryDelay);
            });
    }

    private void getIpCountry(int delayMs) {
        if (!VPNGeneralPersistentData.getShowIpActivated()) {
            return;
        }

        ipSubscription.dispose();

        this.ipSubscription = Observable.just(0).delay(delayMs, TimeUnit.MILLISECONDS).flatMap(v -> ApiClient.getIpCountry(currentIp))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(response -> {
                if (response.body() != null) {
                    progressCountry.setVisibility(GONE);

                    String[] dataParts = response.body().split(";");
                    if (dataParts.length == 4) {
                        textCountry.setText(dataParts[3]);
                    } else {
                        textCountry.setText(getContext().getText(R.string.general_unknown));
                    }

                    previousCountry = textCountry.getText().toString();
                } else {
                    getIpCountry(retryDelay);
                }
            }, err -> {
                getIpCountry(retryDelay);
            });
    }

    @Override
    public void close() {
        serviceSubscription.dispose();
        cancelIpCheck();
    }

    private void cancelIpCheck() {
        if (ipSubscription != null) {
            ipSubscription.dispose();
        }
    }

    @Override
    public void onClick(View view) {
        if (!VPNGeneralPersistentData.getKillSwitchActivated()) {
            VPNCoordinator.getInstance().stopVPN();
        } else {
            ConfirmationModalWindow confirmationModal = new ConfirmationModalWindow(
                getContext(),
                R.string.tmp_status_connected_disconnect_confirmation,
                R.string.tmp_confirmation_yes,
                R.string.tmp_confirmation_no,
                () -> {
                    VPNCoordinator.getInstance().stopVPN();
                    buttonStop.setEnabled(false);
                }
            );
            confirmationModal.show();
        }
    }
}
