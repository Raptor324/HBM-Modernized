package com.hbm_m.blockentity.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code FluidDuctGauge.TileEntityPipeGauge}: liest jeden Tick den Durchsatz ({@code fluidTracker}) des
 * angeschlossenen Rohrnetzes und summiert ihn sekundenweise. Per Redstone-over-Radio abfragbar.
 */
public class FluidDuctGaugeBlockEntity extends PaintableDuctBlockEntity implements IRORValueProvider {

    public long deltaTick = 0;
    private long deltaSecond = 0;
    public long deltaLastSecond = 0;

    public FluidDuctGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DUCT_GAUGE_BE.get(), pos, state);
    }

    public static void gaugeTick(Level level, BlockPos pos, BlockState state, FluidDuctGaugeBlockEntity te) {
        PaintableDuctBlockEntity.serverTick(level, pos, state, te);

        if (!level.isClientSide) {

            FluidNode node = te.getNode();
            if (node != null && node.net != null && te.getFluidType() != Fluids.EMPTY) {

                te.deltaTick = node.net.fluidTracker;
                if (level.getGameTime() % 20 == 0) {
                    te.deltaLastSecond = te.deltaSecond;
                    te.deltaSecond = 0;
                }
                te.deltaSecond += te.deltaTick;
            }

            // networkPackNT(25)
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("deltaTick", deltaTick);
        tag.putLong("deltaLastSecond", deltaLastSecond);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.deltaTick = Math.max(tag.getLong("deltaTick"), 0);
        this.deltaLastSecond = Math.max(tag.getLong("deltaLastSecond"), 0);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "deltatick",
                PREFIX_VALUE + "deltasecond",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick;
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaLastSecond;
        return null;
    }
}
