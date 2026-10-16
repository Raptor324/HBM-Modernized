package com.hbm_m.api.energy;

import com.hbm_m.api.network.INetworkProvider;

/**
 * Порт com.hbm.uninos.networkproviders.PowerNetProvider из 1.7.10.
 */
public class PowerNetProvider implements INetworkProvider<PowerNet> {

    @Override
    public PowerNet createNetwork() {
        return new PowerNet();
    }
}
