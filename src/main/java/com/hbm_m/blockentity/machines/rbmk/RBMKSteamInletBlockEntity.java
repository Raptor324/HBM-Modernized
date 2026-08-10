package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 port of TileEntityRBMKInlet.
 * Floor-level block (NOT an RBMK column). Accepts water and feeds it into the
 * reasimWater of adjacent RBMK columns when the ReaSim boiler dial is active.
 */
public class RBMKSteamInletBlockEntity extends BlockEntity {

    public final FluidTank waterTank = new FluidTank(32_000);

    public RBMKSteamInletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STEAM_INLET_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKSteamInletBlockEntity be) {
        if (level.isClientSide) return;

        // Distribute water to adjacent RBMK columns (horizontal directions only)
        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof RBMKColumnBlockEntity col) {
                int give = Math.min(col.MAX_WATER - col.reasimWater, be.waterTank.getFill());
                if (give > 0) {
                    col.reasimWater += give;
                    be.waterTank.setFill(be.waterTank.getFill() - give);
                    be.setChanged();
                }
            }
        }
    }

    // ─── NBT / Sync ──────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        waterTank.writeToNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        waterTank.writeToNBT(tag, "tank");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        waterTank.readFromNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        waterTank.readFromNBT(tag, "tank");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {

        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
