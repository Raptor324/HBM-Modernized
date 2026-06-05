package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.OilDrillBaseBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachineDerrickMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineDerrickBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER     = 100_000L;
    private static final int  CONSUMPTION   = 100;
    private static final int  DELAY         = 50;

    private static final int OIL_PER_DEPOSIT     = 1000;
    private static final int GAS_PER_DEPOSIT_MIN = 100;
    private static final int GAS_PER_DEPOSIT_MAX = 500;

    public MachineDerrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DERRICK_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDerrickBlockEntity blockEntity) {
        OilDrillBaseBlockEntity.tick(level, pos, state, blockEntity);
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
        level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 2.0F, 0.5F);

        int gas = GAS_PER_DEPOSIT_MIN + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1);
        tanks[0].fillMb(ModFluids.CRUDE_OIL.getSource(), OIL_PER_DEPOSIT);
        tanks[1].fillMb(ModFluids.GAS.getSource(), gas);

        // one pump exhausts the deposit completely
        level.setBlockAndUpdate(pos, com.hbm_m.block.ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState());
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
        return Component.translatable("block.hbm_m.derrick");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineDerrickMenu.create(id, inventory, this);
    }
}
