package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.menu.HeatingOvenMenu;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityHeaterOven}: Feuerbuechse mit 500 TU/t Grundhitze, 500.000 TU Speicher und einem Achtel der
 * Brennzeit; zieht ausserdem Waerme aus einer Waermequelle direkt darunter (Wirkungsgrad 0,5).
 */
public class HeatingOvenBlockEntity extends MachineFireboxBaseBlockEntity {

    public static int baseHeat = 500;
    public static double timeMult = 0.125D;
    public static int maxHeatEnergy = 500_000;
    public static double heatEff = 0.5D;
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

    public HeatingOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_OVEN_BE.get(), pos, state);
    }

    @Override
    protected void serverTick(Level level, BlockPos pos) {
        this.tryPullHeat(level, pos);
        super.serverTick(level, pos);
    }

    protected void tryPullHeat(Level level, BlockPos pos) {
        BlockEntity con = level.getBlockEntity(pos.below());

        if (con instanceof IHeatSource source) {
            int toPull = Math.max(Math.min(source.getHeatStored(), this.getMaxHeat() - this.heatEnergy), 0);
            this.heatEnergy += toPull * heatEff;
            source.useUpHeat(toPull);
        }
    }

    @Override public ModuleBurnTime getModule() { return burnModule; }
    @Override public int getBaseHeat() { return baseHeat; }
    @Override public double getTimeMult() { return timeMult; }
    @Override public int getMaxHeat() { return maxHeatEnergy; }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.heating_oven");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new HeatingOvenMenu(containerId, playerInventory, this);
    }
}
