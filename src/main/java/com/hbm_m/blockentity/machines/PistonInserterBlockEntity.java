package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.api.block.IInsertable;
import com.hbm_m.block.machines.PistonInserterBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code PistonInserter.TileEntityPistonInserter}: haelt einen Gegenstand; ein Redstone-Impuls faehrt den Kolben
 * in 25 Ticks aus und drueckt ihn in den {@link IInsertable}-Block zwei Felder voraus, dann 5 Ticks Pause und Rueckzug.
 */
public class PistonInserterBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    public ItemStack slot = ItemStack.EMPTY;

    public int extend;
    public static final int maxExtend = 25;
    public boolean isRetracting = true;
    public int delay;

    //prevents funkies from happening with block updates or loading into a server
    public boolean lastState;

    public double renderExtend;
    public double lastExtend;
    private int syncExtend;
    private int turnProgress;

    public PistonInserterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PISTON_INSERTER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PistonInserterBlockEntity be) {
        be.update(level, pos, state);
    }

    private void update(Level world, BlockPos pos, BlockState state) {

        if (!world.isClientSide) {

            int prevExtend = extend;

            if (delay <= 0) {

                if (this.isRetracting && this.extend > 0) {
                    this.extend--;
                } else if (!this.isRetracting) {
                    this.extend++;

                    if (this.extend >= maxExtend) {
                        world.playSound(null, pos, HbmSoundsNT.get("hbm:block.pressOperate"), SoundSource.BLOCKS, 1.0F, 1.5F);

                        Direction dir = state.getValue(PistonInserterBlock.FACING);
                        BlockPos target = pos.relative(dir, 2);

                        if (world.getBlockState(target).getBlock() instanceof IInsertable ins && ins.insertItem(world, target, dir, slot)) {
                            this.slot.shrink(1);
                            if (this.slot.isEmpty()) this.slot = ItemStack.EMPTY;
                        }

                        this.isRetracting = true;
                        this.delay = 5;
                    }
                }

            } else {
                delay--;
            }

            if (extend != prevExtend || world.getGameTime() % 20 == 0) markForSync();

        } else {
            this.lastExtend = this.renderExtend;

            if (this.turnProgress > 0) {
                this.renderExtend += (this.syncExtend - this.renderExtend) / (double) this.turnProgress;
                this.turnProgress--;
            } else {
                this.renderExtend = this.syncExtend;
            }
        }
    }

    public void markForSync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putInt("extend", extend);
        nbt.putBoolean("retract", isRetracting);
        nbt.putBoolean("state", lastState); //saved so loading into a world doesn't cause issues
        if (!this.slot.isEmpty()) {
            CompoundTag stack = new CompoundTag();
            com.hbm_m.platform.StackNbt.save(slot, stack);
            nbt.put("stack", stack);
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.extend = nbt.getInt("extend");
        this.isRetracting = nbt.getBoolean("retract");
        this.lastState = nbt.getBoolean("state");
        this.slot = nbt.contains("stack") ? StackNbt.parse(nbt.getCompound("stack")) : ItemStack.EMPTY;
        // Client: wie deserialize (Zielwert, 2 Ticks interpolieren)
        this.syncExtend = this.extend;
        this.turnProgress = 2;
    }

    @Override
    public AABB getRenderBoundingBox() {
        Direction dir = getBlockState().getValue(PistonInserterBlock.FACING);
        return new AABB(worldPosition).expandTowards(dir.getStepX(), dir.getStepY(), dir.getStepZ());
    }
}
