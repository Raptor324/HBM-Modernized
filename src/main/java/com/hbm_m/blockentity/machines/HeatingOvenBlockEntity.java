package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code TileEntityHeaterOven} (1.7.10) — «Heating Oven», продвинутый аналог
 * гритбокса: горит в 5 раз горячее (baseHeat 500), но топливо сгорает в 8 раз
 * быстрее (timeMult 0.125), теплоёмкость 500k TU. Медный контакт снизу позволяет
 * ставить печи в стек — тепло снизу принимается с КПД 50% ({@code heatEff}).
 */
public class HeatingOvenBlockEntity extends FireboxBaseBlockEntity {

    private static final int BASE_HEAT = 500;
    private static final double TIME_MULT = 0.125D;
    private static final int MAX_HEAT_ENERGY = 500_000;
    private static final double HEAT_PULL_EFF = 0.5D;

    /** Порт burnModule из TileEntityHeaterOven (та же таблица, что у гритбокса). */
    public static final ModuleBurnTime BURN_MODULE = new ModuleBurnTime()
            .setLigniteTimeMod(1.25).setCoalTimeMod(1.25).setCokeTimeMod(1.25)
            .setSolidTimeMod(1.5).setRocketTimeMod(1.5).setBalefireTimeMod(0.5)
            .setLigniteHeatMod(2).setCoalHeatMod(2).setCokeHeatMod(2)
            .setSolidHeatMod(3).setRocketHeatMod(5).setBalefireHeatMod(15);

    public HeatingOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_OVEN_BE.get(), pos, state);
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

    /** Тикер сервера: сначала затяжка тепла снизу, затем базовая логика гритбокса. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HeatingOvenBlockEntity be) {
        be.tryPullHeatFromBelow();
        FireboxBaseBlockEntity.serverTick(level, pos, state, be);
    }

    /** Тикер клиента: только анимация дверцы/частицы. */
    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HeatingOvenBlockEntity be) {
        FireboxBaseBlockEntity.clientTick(level, pos, state, be);
    }

    /** Порт tryPullHeat — вызывается из блочного тикера перед serverTick базы. */
    public void tryPullHeatFromBelow() {
        if (level == null || level.isClientSide()) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int toPull = Math.max(Math.min(source.getHeatStored(), this.getMaxHeat() - this.heatEnergy), 0);
            this.heatEnergy += (int) (toPull * HEAT_PULL_EFF);
            source.useUpHeat(toPull);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.heating_oven");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public com.hbm_m.inventory.menu.HeatingOvenMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new com.hbm_m.inventory.menu.HeatingOvenMenu(id, inventory, this);
    }
}
