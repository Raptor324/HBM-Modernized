package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code TileEntityHeaterFirebox} (1.7.10) — самый дешёвый внешний источник
 * тепла: baseHeat 100 TU/t, теплоёмкость 100k TU, модули топлива
 * (лигнит/уголь/кокс ×1.25 времени ×2 тепла; твёрдое ×1.5/×3; ракетное ×1.5/×5;
 * бэйлфаер ×0.5/×15).
 */
public class MachineFireboxBlockEntity extends FireboxBaseBlockEntity {

    public static final int INVENTORY_SIZE = FireboxBaseBlockEntity.FUEL_SLOTS;

    private static final int BASE_HEAT = 100;
    private static final double TIME_MULT = 1.0D;
    private static final int MAX_HEAT_ENERGY = 100_000;

    /** Порт burnModule из TileEntityHeaterFirebox. */
    public static final ModuleBurnTime BURN_MODULE = new ModuleBurnTime()
            .setLigniteTimeMod(1.25).setCoalTimeMod(1.25).setCokeTimeMod(1.25)
            .setSolidTimeMod(1.5).setRocketTimeMod(1.5).setBalefireTimeMod(0.5)
            .setLigniteHeatMod(2).setCoalHeatMod(2).setCokeHeatMod(2)
            .setSolidHeatMod(3).setRocketHeatMod(5).setBalefireHeatMod(15);

    public MachineFireboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREBOX_BE.get(), pos, state);
    }

    @Override
    public ModuleBurnTime getModule() {
        return BURN_MODULE;
    }

    @Override
    public int getBaseHeat() {
        return BASE_HEAT;
    }

    @Override
    public double getTimeMult() {
        return TIME_MULT;
    }

    @Override
    public int getMaxHeat() {
        return MAX_HEAT_ENERGY;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.firebox");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public com.hbm_m.inventory.menu.MachineFireboxMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new com.hbm_m.inventory.menu.MachineFireboxMenu(id, inventory, this);
    }
}
