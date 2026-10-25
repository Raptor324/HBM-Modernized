package com.hbm_m.api.rebar;

import com.hbm_m.api.network.INetworkProvider;

/** 1:1 {@code com.hbm.uninos.networkproviders.RebarNetworkProvider}. */
public class RebarNetworkProvider implements INetworkProvider<RebarNetwork> {

    public static final RebarNetworkProvider THE_PROVIDER = new RebarNetworkProvider();

    @Override
    public RebarNetwork createNetwork() {
        return new RebarNetwork();
    }
}
