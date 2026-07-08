package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKNeutronStream;

public interface IRBMKFluxReceiver {

    enum NType {
        FAST("trait.rbmk.neutron.fast"),
        SLOW("trait.rbmk.neutron.slow"),
        ANY("trait.rbmk.neutron.any");

        public final String unlocalized;

        NType(String loc) {
            this.unlocalized = loc;
        }
    }

    void receiveFlux(RBMKNeutronStream stream);
}

