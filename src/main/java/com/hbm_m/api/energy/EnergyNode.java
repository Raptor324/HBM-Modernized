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
     * Проверяет, валиден ли узел.
     * ВАЖНО: Если чанк не загружен, мы считаем узел "условно валидным",
     * чтобы не разрушать сеть в выгруженных областях мира.
     */
    public boolean isValid(ServerLevel level) {
        // [ИСПРАВЛЕНИЕ] Если чанк не загружен, мы не можем проверить наличие блока.
        // Возвращаем true, чтобы сохранить узел в памяти менеджера.
        // Он будет проверен позже, когда чанк прогрузится и сеть тикнет.
        if (!level.isLoaded(pos)) return true;

        BlockEntity be = level.getBlockEntity(pos);
        // Если чанк загружен, но TileEntity нет — значит блок сломали, удаляем узел.
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