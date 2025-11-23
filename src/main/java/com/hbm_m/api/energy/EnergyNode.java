package com.hbm_m.api.energy;

import com.hbm_m.capability.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Узел энергетической сети (провод или машина)
 */
public class EnergyNode {
    private final BlockPos pos;
    private EnergyNetwork network;

    public EnergyNode(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public EnergyNetwork getNetwork() {
        return network;
    }

    public void setNetwork(EnergyNetwork network) {
        this.network = network;
    }

    /**
     * Проверяет, валиден ли узел (загружен ли чанк, есть ли BlockEntity с capability)
     */
    public boolean isValid(ServerLevel level) {
        if (!level.isLoaded(pos)) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        return be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent() ||
                be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).isPresent() ||
                be.getCapability(ModCapabilities.HBM_ENERGY_CONNECTOR).isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnergyNode node)) return false;
        return pos.equals(node.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}
