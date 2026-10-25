package com.hbm_m.api.rebar;

import com.hbm_m.api.network.NodeNet;
import com.hbm_m.block.generic.BlockRebar.RebarNode;

import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code com.hbm.uninos.networkproviders.RebarNetwork}: reines Verbindungsnetz, die Verteilung macht die Bewehrung selbst. */
public class RebarNetwork extends NodeNet<BlockEntity, BlockEntity, RebarNode> {

    @Override
    public void update() { }
}
