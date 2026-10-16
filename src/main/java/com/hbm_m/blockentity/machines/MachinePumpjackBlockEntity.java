package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.OilDrillBaseBlockEntity;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachinePumpjackMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pumpjack: Portierung von {@code TileEntityMachinePumpjack} (1.7.10 Original). Erbt die komplette Bohr-/Saug-Logik
 * von {@link OilDrillBaseBlockEntity} (bereits fuer {@link MachineDerrickBlockEntity} etabliert), im Unterschied
 * zum Derrick wird das Oelvorkommen aber nicht garantiert bei jedem Saugvorgang geleert, sondern nur mit
 * {@link #DRAIN_CHANCE} Wahrscheinlichkeit (Direktport von {@code onSuck}).
 */
public class MachinePumpjackBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER = 250_000L;
    private static final int CONSUMPTION = 200;
    private static final int DELAY = 25;

    private static final int OIL_PER_DEPOSIT = 750;
    private static final int GAS_PER_DEPOSIT_MIN = 50;
    private static final int GAS_PER_DEPOSIT_MAX = 250;
    private static final double DRAIN_CHANCE = 0.025D;

    /** Client-seitige Kolben-Rotation (Direktport von {@code rot}/{@code prevRot}/{@code speed}). */
    public float rot = 0F;
    public float prevRot = 0F;

    public MachinePumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMPJACK_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePumpjackBlockEntity be) {
        if (level.isClientSide) {
            be.prevRot = be.rot;
            if (be.indicator == 0) {
                be.rot += 5F + (2F * be.speedLevel) + (be.overLevel - 1F) * 10F;
            }
            if (be.rot >= 360F) {
                be.rot -= 360F;
                be.prevRot -= 360F;
            }
            return;
        }
        OilDrillBaseBlockEntity.tick(level, pos, state, be);
    }

    @Override
    public int getPowerReq() {
        return CONSUMPTION;
    }

    @Override
    public int getDelay() {
        return DELAY;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void onSuck(BlockPos pos) {
        tanks[0].fillMb(ModFluids.CRUDE_OIL.getSource(), OIL_PER_DEPOSIT);

        int gas = GAS_PER_DEPOSIT_MIN + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1);
        tanks[1].fillMb(ModFluids.GAS.getSource(), gas);

        if (level.getRandom().nextDouble() < DRAIN_CHANCE) {
            level.setBlockAndUpdate(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState());
        }
    }

    @Override
    public Direction[] getConPos() {
        return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pumpjack");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    // ==================== GUI (generische GuiInfoScreen-Balken) ====================

    public int getProgress() {
        if (level == null) return 0;
        int delay = Math.max(1, getDelayEff());
        return (int) (level.getGameTime() % delay);
    }

    public int getMaxProgress() {
        return Math.max(1, getDelayEff());
    }

    public int getProgressScaled(int scale) {
        int max = getMaxProgress();
        return max <= 0 ? 0 : getProgress() * scale / max;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachinePumpjackMenu.create(id, inventory, this);
    }
}
