package com.hbm_m.api.fusion;

import com.hbm_m.api.network.INetworkProvider;

/** 1:1-Port von {@code com.hbm.uninos.networkproviders.PlasmaNetworkProvider} (1.7.10). */
public class PlasmaNetworkProvider implements INetworkProvider<PlasmaNetwork> {

    public static final PlasmaNetworkProvider THE_PROVIDER = new PlasmaNetworkProvider();

    @Override
    public PlasmaNetwork createNetwork() {
        return new PlasmaNetwork();
    }
}
