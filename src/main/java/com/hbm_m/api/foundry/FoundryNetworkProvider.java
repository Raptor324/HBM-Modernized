package com.hbm_m.api.foundry;

import com.hbm_m.api.network.INetworkProvider;

/** 1:1 {@code FoundryNetworkProvider}. */
public class FoundryNetworkProvider implements INetworkProvider<FoundryNetwork> {

    public static final FoundryNetworkProvider THE_PROVIDER = new FoundryNetworkProvider();

    @Override
    public FoundryNetwork createNetwork() {
        return new FoundryNetwork();
    }
}
