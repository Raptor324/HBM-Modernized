package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.PileBlock;
import com.hbm_m.block.machines.pile.PileBlockType;
import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1-Port von {@code TileEntityPileVent} (1.7.10): das Geblaese am Eingang eines Lueftungskanals.
 *
 * <p>Es zieht Druckluft aus dem Rohrnetz und schiebt sie in den Kanal dahinter, bis dieser seine
 * tausend Millibucket fasst. Der Kern rechnet daraus die Kuehlung aller Brennstoffkanaele auf
 * gleicher Hoehe. Ohne Geblaese laeuft ein Meiler nur wenige Minuten, bevor er durchgeht.</p>
 */
public class PileVentBlockEntity extends LoadedMachineBlockEntity implements IFluidStandardReceiverMK2 {

    /** Original: {@code new FluidTank(Fluids.AIR, 4_000).withPressure(1)}. */
    private static final int TANK_CAPACITY = 4_000;

    private final FluidTank compair;
    private boolean isActive = false;
    private int chanNum;

    /** Nur zur Darstellung: der Winkel des Luefterrads. */
    private float fan;
    private float lastFan;

    public PileVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_VENT_BE.get(), pos, state);
        this.compair = new FluidTank(ModFluids.AIR.getSource(), TANK_CAPACITY).withPressure(1);
    }

    public Direction getOrientation() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    public boolean isActive()  { return isActive; }
    public float getFan()      { return fan; }
    public float getLastFan()  { return lastFan; }
    public int getChannelNum() { return chanNum; }
    public FluidTank getTank() { return compair; }

    public static void tick(Level level, BlockPos pos, BlockState state, PileVentBlockEntity be) {

        if (level.isClientSide()) {
            be.lastFan = be.fan;
            if (be.isActive) be.fan += 45F;
            if (be.fan >= 360F) {
                be.lastFan -= 360F;
                be.fan -= 360F;
            }
            return;
        }

        Direction dir = be.getOrientation();
        be.trySubscribe(be.compair.getTankType(), level, pos.relative(dir), dir);

        be.isActive = false;

        // Der Kanal liegt hinter dem Geblaese, also entgegen der Blickrichtung.
        BlockPos channelPos = pos.relative(dir.getOpposite());
        BlockState channelState = level.getBlockState(channelPos);

        if (channelState.is(ModBlocks.PILE_BLOCK.get())
                && channelState.hasProperty(PileBlock.TYPE)
                && channelState.getValue(PileBlock.TYPE) == PileBlockType.AIR_IN) {

            BlockEntity tile = level.getBlockEntity(channelPos);
            if (tile instanceof PileBaseBlockEntity pile) {
                PileCoreBlockEntity core = pile.getCore(level);

                if (core != null) {
                    PileChannel ventChan = core.getVentilationChannel(channelPos);
                    if (ventChan != null) {
                        be.chanNum = core.getVentilationChannelNum(ventChan);

                        int toFill = Math.min(be.compair.getFill(), PileChannel.MAX_AIR - ventChan.air);
                        if (toFill > 0) {
                            ventChan.air += toFill;
                            be.compair.setFill(be.compair.getFill() - toFill);
                            be.isActive = true;
                        }
                    }
                }
            }
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    // ── IFluidStandardReceiverMK2 ───────────────────────────────────────────

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { compair };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { compair };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Original: das Geblaese nimmt Luft nur an seiner Vorderseite an. */
    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        if (type != compair.getTankType()) return false;
        return dir == getOrientation();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        compair.writeToNBT(tag, "t");
        tag.putBoolean("active", isActive);
        tag.putInt("chanNum", chanNum);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        compair.readFromNBT(tag, "t");
        isActive = tag.getBoolean("active");
        chanNum = tag.getInt("chanNum");
    }
}
