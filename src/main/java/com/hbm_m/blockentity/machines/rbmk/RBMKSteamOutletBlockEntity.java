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
 * 1:1 port of {@code TileEntityRBMKOutlet}.
 *
 * <p>Floor-level block (NOT an RBMK column). Drains the {@code reasimSteam} out of the four
 * horizontally adjacent RBMK columns while the ReaSim boiler dial is on, and pushes it into the
 * fluid network on every face.</p>
 */
public class RBMKSteamOutletBlockEntity extends BlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardSenderMK2 {

    /**
     * CE types this tank as <b>superhot</b> steam ({@code new FluidTankNTM(Fluids.SUPERHOTSTEAM, 32000)}),
     * not ordinary steam: the ReaSim channels boil at column temperature, so what comes out of the
     * outlet is worth far more per mB than what a plain steam channel produces. The port had it as
     * ordinary steam, which both undervalued the output and made the outlet refuse to talk to a
     * superhot-steam pipe network.
     */
    public final FluidTank steamTank =
            new FluidTank(com.hbm_m.inventory.fluid.ModFluids.SUPERHOTSTEAM.getSource(), 32_000);

    public RBMKSteamOutletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_STEAM_OUTLET_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKSteamOutletBlockEntity be) {
        if (level.isClientSide) return;

        // 1:1 with CE: the whole transfer only runs while the ReaSim boiler dial is on.
        if (com.hbm_m.handler.rbmk.RBMKDials.getReasimBoilers(level)) {
            for (Direction dir : new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
                RBMKColumnBlockEntity col = RBMKSteamInletBlockEntity.findColumnCore(level, pos.relative(dir));
                if (col == null) continue;
                int take = Math.min(be.steamTank.getMaxFill() - be.steamTank.getFill(), col.reasimSteam);
                if (take > 0) {
                    col.reasimSteam -= take;
                    be.steamTank.setFill(be.steamTank.getFill() + take);
                    col.setChanged();
                    be.setChanged();
                }
            }
        }

        // CE's fillFluidInit: push into every neighbour, unconditionally - the outlet is a sender,
        // it does not wait to be drained. The port only exposed a capability, so steam sat in the
        // tank unless something actively pulled from it.
        if (be.steamTank.getFill() > 0) {
            for (Direction dir : Direction.values()) {
                be.tryProvide(be.steamTank, level, pos.relative(dir), dir);
            }
        }
    }

    // ─── MK2 fluid network ───────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()     { return new FluidTank[] { steamTank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { steamTank }; }

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
            return steamTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ─── NBT / Sync ──────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        steamTank.writeToNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        steamTank.writeToNBT(tag, "tank");
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steamTank.readFromNBT(tag, "tank");
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
