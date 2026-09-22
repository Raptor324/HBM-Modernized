package com.hbm_m.api.fusion;

import com.hbm_m.api.network.INetworkProvider;

/** 1:1-Port von {@code com.hbm.uninos.networkproviders.KlystronNetworkProvider} (1.7.10). */
public class KlystronNetworkProvider implements INetworkProvider<KlystronNetwork> {

    public static final KlystronNetworkProvider THE_PROVIDER = new KlystronNetworkProvider();

    @Override
    public KlystronNetwork createNetwork() {
        return new KlystronNetwork();
    }
}
