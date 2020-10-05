package com.skywire.skycoin.vpn.vpn;

import android.net.VpnService;

import com.skywire.skycoin.vpn.HelperFunctions;
import com.skywire.skycoin.vpn.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.BooleanSupplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import skywiremob.Skywiremob;

public class VPNRunnable {
    private VpnService service;
    private VPNWorkInterface vpnInterface;

    private VisorRunnable visor;
    private SkywireVPNConnection vpnConnection;

    private boolean waitAvailableFinished = false;
    private boolean waitNetworkFinished = false;

    private Disposable waitingSubscription;
    private Disposable visorTimeoutSubscription;

    private boolean disconnectionStarted = false;
    private int disconnectionVerifications = 0;

    private BehaviorSubject<Integer> eventsSubject = BehaviorSubject.create();
    private Observable<Integer> eventsObservable;

    private String lastErrorMsg;

    public VPNRunnable(VpnService service, VPNWorkInterface vpnInterface) {
        eventsSubject.onNext(VPNStates.OFF);
        this.service = service;
        this.vpnInterface = vpnInterface;
    }

    public Observable<Integer> start() {
        if (eventsObservable == null) {
            eventsSubject.onNext(VPNStates.STARTING);
            eventsObservable = eventsSubject.hide();
        }

        waitForVisorToBeAvailableIfNeeded();

        return eventsObservable;
    }

    private void waitForVisorToBeAvailableIfNeeded() {
        if (!waitAvailableFinished) {
            if (waitingSubscription != null) {
                waitingSubscription.dispose();
            }

            if (!Skywiremob.isVisorStarting() && !Skywiremob.isVisorRunning()) {
                waitAvailableFinished = true;
                checkInternetConnectionIfNeeded(true);
            } else {
                if (eventsSubject.getValue() != VPNStates.WAITING_PREVIOUS_INSTANCE_STOP) {
                    Skywiremob.printString("WAITING FOR THE PREVIOUS INSTANCE TO BE FULLY STOPPED");
                    eventsSubject.onNext(VPNStates.WAITING_PREVIOUS_INSTANCE_STOP);
                }

                waitingSubscription = Observable.just(0).delay(1000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(val -> waitForVisorToBeAvailableIfNeeded());
            }
        }
    }

    private void checkInternetConnectionIfNeeded(boolean firstTry) {
        if (!waitNetworkFinished) {
            Skywiremob.printString("CHECKING CONNECTION");

            if (eventsSubject.getValue() != VPNStates.WAITING_FOR_CONNECTIVITY) {
                eventsSubject.onNext(VPNStates.CHECKING_CONNECTIVITY);
            }

            if (waitingSubscription != null) {
                waitingSubscription.dispose();
            }

            waitingSubscription = HelperFunctions.checkInternetConnectivity(firstTry)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hasInternetConnection -> {
                    if (hasInternetConnection) {
                        waitNetworkFinished = true;
                        startVisorIfNeeded();
                    } else {
                        eventsSubject.onNext(VPNStates.WAITING_FOR_CONNECTIVITY);

                        waitingSubscription.dispose();
                        waitingSubscription = Observable.just(0).delay(1000, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(val -> checkInternetConnectionIfNeeded(false));
                    }
                });
        }
    }

    private void startVisorIfNeeded() {
        if (visor == null) {
            Skywiremob.printString("STARTING VISOR");

            visor = new VisorRunnable();

            if (waitingSubscription != null) {
                waitingSubscription.dispose();
            }

            waitingSubscription = visor.runVisor()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    eventsSubject.onNext(state);

                    if (visorTimeoutSubscription != null) {
                        visorTimeoutSubscription.dispose();
                    }

                    visorTimeoutSubscription = Observable.just(0).delay(45000, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(val -> {
                            HelperFunctions.logError("VPN service", "Timeout preparing the visor.");
                            putInErrorState(service.getString(R.string.vpn_timerout_error));
                        });
                }, err -> {
                    putInErrorState(err.getLocalizedMessage());
                }, () -> {
                    visorTimeoutSubscription.dispose();
                    startConnection();
                });
        }
    }

    private void startConnection() {
        vpnConnection = new SkywireVPNConnection(service, visor, vpnInterface);

        waitingSubscription.dispose();

        waitingSubscription = vpnConnection.getObservable()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                val -> {
                    eventsSubject.onNext(val);
                }, err -> {
                    putInErrorState(err.getLocalizedMessage());
                }, () -> {
                    // This event is not expected, but it would mean that the vpn connection is not longer active.
                    HelperFunctions.logError("VPN connection ended unexpectedly", "VPN connection ended unexpectedly");
                    disconnect();
                }
            );
    }

    public void disconnect() {
        if (!disconnectionStarted) {
            disconnectionStarted = true;

            Skywiremob.printString("DISCONNECTING VPN RUNNABLE");

            eventsSubject.onNext(VPNStates.DISCONNECTING);

            if (waitingSubscription != null) {
                waitingSubscription.dispose();
            }
            if (visorTimeoutSubscription != null) {
                visorTimeoutSubscription.dispose();
            }
            if (this.vpnConnection != null) {
                this.vpnConnection.close();
            }

            Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
                if (visor != null) {
                    visor.startStoppingVisor();
                }
                emitter.onComplete();
            }).subscribeOn(Schedulers.newThread()).subscribe(val -> {});

            Observable.timer(100, TimeUnit.MILLISECONDS).repeatUntil(new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    if (!Skywiremob.isVisorStarting() && !Skywiremob.isVisorRunning()) {
                        if (disconnectionVerifications == 2) {
                            return true;
                        } else {
                            disconnectionVerifications += 1;
                        }
                    } else {
                        if (disconnectionVerifications != 0) {
                            if (visor != null) {
                                visor.startStoppingVisor();
                            }
                        }

                        disconnectionVerifications = 0;
                    }

                    return false;
                }
            })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(val -> {}, err -> {}, () -> eventsSubject.onNext(VPNStates.DISCONNECTED));
        }
    }

    private void putInErrorState(String errorMsg) {
        lastErrorMsg = errorMsg;

        if (!vpnInterface.alreadyConfigured() || !VPNPersistentData.getKillSwitchActivated()) {
            eventsSubject.onNext(VPNStates.ERROR);
        } else {
            eventsSubject.onNext(VPNStates.BLOCKING_ERROR);
        }

        disconnect();
    }

    public String getLastErrorMsg() {
        return lastErrorMsg;
    }
}
