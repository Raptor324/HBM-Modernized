package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of the 1.7.10 TileEntityFoundryMold (extends TileEntityFoundryCastingBase - identical
 * casting behaviour to the basin, only the block's collision shape/rendering differs). Reuses
 * {@link MachineFoundryBasinBlockEntity}'s logic wholesale via its protected constructor.
 */
public class MachineFoundryMoldBlockEntity extends MachineFoundryBasinBlockEntity {

    public MachineFoundryMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_MOLD_BE.get(), pos, state);
    }

    /** Малая форма — порт TileEntityFoundryMold.getMoldSize (только слитки-формы размера 0). */
    @Override
    public int getMoldSize() { return 0; }

    /**
     * Оригинал: TileEntityFoundryMold НЕ переопределяет canAcceptPartialFlow/flow и наследует
     * их из TileEntityFoundryBase (standardCheck/standardAdd) — поэтому канал может заливать
     * расплав в малый бассейн НАПРЯМУЮ, без литейного спуска. Глубокий foundry_basin, в отличие
     * от формы, боковой поток отвергает явным override'ом — см. MachineFoundryBasinBlockEntity.
     */
    @Override
    public boolean canAcceptPartialFlow(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                        net.minecraft.core.Direction side, com.hbm_m.inventory.material.MaterialStack stack) {
        return standardCheck(stack);
    }

    @Override
    public @org.jetbrains.annotations.Nullable com.hbm_m.inventory.material.MaterialStack flow(
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side,
            com.hbm_m.inventory.material.MaterialStack stack) {
        return standardAdd(stack);
    }

    // Рендер-геометрия 1:1 с TileEntityFoundryMold (IRenderFoundry): мелкая чаша.
    @Override public float getSurfaceRange() { return 0.25f; }
    @Override public float getOutputHeight() { return 0.25f; }
}
