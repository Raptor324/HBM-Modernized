package com.hbm_m.api.fusion;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeNet;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code com.hbm.uninos.networkproviders.PlasmaNetwork} (1.7.10).
 * Reines Transportnetz ohne eigene Tick-Logik - der Torus verteilt die Leistung selbst
 * ueber {@link IFusionPowerReceiver}.
 */
public class PlasmaNetwork extends NodeNet<BlockEntity, BlockEntity, GenNode<PlasmaNetwork>> {

    @Override
    public void update() { }
}
