package com.hbm_m.api.energy;

import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.interfaces.IEnergyConnector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Маркер проводника энергосети (аналог IEnergyConductorMK2 из 1.7.10).
 * Проводники создают узлы (PowerNode) в UniNodespace.
 */
public interface PowerConductor extends IEnergyConnector {

    /**
     * Аналог createNode из 1.7.10: узел с шестью точками соединения (по всем граням).
     */
    default Nodespace.PowerNode createNode(BlockPos pos) {
        return new Nodespace.PowerNode(Nodespace.THE_POWER_PROVIDER, pos).setConnections(
                new NodeDirPos(pos.getX() + 1, pos.getY(), pos.getZ(), Direction.EAST),
                new NodeDirPos(pos.getX() - 1, pos.getY(), pos.getZ(), Direction.WEST),
                new NodeDirPos(pos.getX(), pos.getY() + 1, pos.getZ(), Direction.UP),
                new NodeDirPos(pos.getX(), pos.getY() - 1, pos.getZ(), Direction.DOWN),
                new NodeDirPos(pos.getX(), pos.getY(), pos.getZ() + 1, Direction.SOUTH),
                new NodeDirPos(pos.getX(), pos.getY(), pos.getZ() - 1, Direction.NORTH)
        );
    }
}
