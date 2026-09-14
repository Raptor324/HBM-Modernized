package com.hbm_m.api.fusion;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeNet;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code com.hbm.uninos.networkproviders.KlystronNetwork} (1.7.10).
 * Reines Transportnetz ohne eigene Tick-Logik - die Klystrons schieben ihre Energie
 * direkt in den als Receiver eingetragenen Torus.
 */
public class KlystronNetwork extends NodeNet<BlockEntity, BlockEntity, GenNode<KlystronNetwork>> {

    @Override
    public void update() { }
}
