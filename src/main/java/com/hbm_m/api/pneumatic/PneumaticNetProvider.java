package com.hbm_m.api.pneumatic;

import com.hbm_m.api.network.INetworkProvider;

/**
 * 1:1-Port von {@code PneumaticNetworkProvider} (1.7.10). Es gibt nur eine Art Rohrnetz, darum
 * genuegt ein einziger Anbieter - er dient zugleich als Schluessel im Knotenraum.
 */
public class PneumaticNetProvider implements INetworkProvider<PneumaticNet> {

    public static final PneumaticNetProvider THE_PROVIDER = new PneumaticNetProvider();

    private PneumaticNetProvider() {}

    @Override
    public PneumaticNet createNetwork() {
        return new PneumaticNet();
    }
}
