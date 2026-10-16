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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 port of {@code TileEntityRBMKInlet}.
 *
 * <p>Floor-level block (NOT an RBMK column). Accepts water and feeds it into the {@code reasimWater}
 * of the four horizontally adjacent RBMK columns while the ReaSim boiler dial is on.</p>
 */
public class RBMKSteamInletBlockEntity extends BlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    public final FluidTank waterTank = new FluidTank(com.hbm_m.inventory.fluid.ModFluids.WATER.getSource(), 32_000);

    public RBMKSteamInletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STEAM_INLET_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKSteamInletBlockEntity be) {
        if (level.isClientSide) return;

        // CE's subscribeToAllAround: the inlet joins whatever water network touches it, on every
        // face. Without this it could only ever be filled by a block physically shoved against it -
        // a pipe run leading to it did nothing.
        for (Direction dir : Direction.values()) {
            be.trySubscribe(be.waterTank.getTankType(), level, pos.relative(dir), dir);
        }

        // 1:1 with CE: the whole transfer only runs while the ReaSim boiler dial is on
        // (TileEntityRBMKInlet/Outlet.update both gate on getReasimBoilers).
        if (!com.hbm_m.handler.rbmk.RBMKDials.getReasimBoilers(level)) return;

        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
            RBMKColumnBlockEntity col = findColumnCore(level, pos.relative(dir));
            if (col == null) continue;
            int give = Math.min(RBMKColumnBlockEntity.MAX_WATER - col.reasimWater, be.waterTank.getFill());
            if (give > 0) {
                col.reasimWater += give;
                be.waterTank.setFill(be.waterTank.getFill() - give);
                col.setChanged();
                be.setChanged();
            }
        }
    }

    /**
     * CE resolves the neighbour through {@code RBMKBase.findCore}, so an inlet does not have to sit
     * at the exact height of the column's base block - hitting any of the column's dummy segments
     * finds the real block entity underneath. The port used to test the neighbour position
     * directly, which silently did nothing whenever the inlet was one block too high.
     */
    public static RBMKColumnBlockEntity findColumnCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RBMKColumnBlockEntity col) return col;

        BlockPos cursor = pos;
        int maxHeight = com.hbm_m.handler.rbmk.RBMKDials.getColumnHeight(level);
        for (int i = 0; i < maxHeight; i++) {
            if (!(level.getBlockState(cursor).getBlock()
                    instanceof com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock)) return null;
            cursor = cursor.below();
            if (level.getBlockEntity(cursor) instanceof RBMKColumnBlockEntity found) return found;
        }
        return null;
    }

    // ─── MK2 fluid network ───────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return new FluidTank[] { waterTank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { waterTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return true;
    }

    //? if forge {
    /**
     * Without this the tank existed but nothing could ever reach it - pipes and tanks had no
     * handler to talk to, so the channel simply refused every connection.
     */
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return waterTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ─── NBT / Sync ──────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        waterTank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        waterTank.readFromNBT(tag, "tank");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
