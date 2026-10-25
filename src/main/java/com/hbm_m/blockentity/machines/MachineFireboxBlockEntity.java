package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityHeaterFirebox}: 100 TU/t Grundhitze, Speicher 100.000 TU, Brennzeit x1.
 */
public class MachineFireboxBlockEntity extends MachineFireboxBaseBlockEntity {

    public static int baseHeat = 100;
    public static double timeMult = 1D;
    public static int maxHeatEnergy = 100_000;
    public static ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.25)
            .setSolidTimeMod(1.5)
            .setRocketTimeMod(1.5)
            .setBalefireTimeMod(0.5)

            .setLigniteHeatMod(2)
            .setCoalHeatMod(2)
            .setCokeHeatMod(2)
            .setSolidHeatMod(3)
            .setRocketHeatMod(5)
            .setBalefireHeatMod(15);

    public MachineFireboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREBOX_BE.get(), pos, state);
    }

    /** Fuer Ziegelofen u.a. - siehe {@link MachineFireboxBaseBlockEntity#getAshFromFuel}. */
    public static MachineAshpitBlockEntity.AshType ashFromFuel(ItemStack stack) {
        return getAshFromFuel(stack);
    }

    @Override public ModuleBurnTime getModule() { return burnModule; }
    @Override public int getBaseHeat() { return baseHeat; }
    @Override public double getTimeMult() { return timeMult; }
    @Override public int getMaxHeat() { return maxHeatEnergy; }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.firebox");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineFireboxMenu.create(id, inventory, this);
    }
}
