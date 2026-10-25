package com.hbm_m.api.foundry;

import com.hbm_m.api.network.NodeNet;

import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code FoundryNetwork}: reines Verbindungsnetz der Giessrinnen, die Verteilung machen die Rinnen selbst. */
public class FoundryNetwork extends NodeNet<BlockEntity, BlockEntity, FoundryNode> {

    @Override
    public void update() { }
}
