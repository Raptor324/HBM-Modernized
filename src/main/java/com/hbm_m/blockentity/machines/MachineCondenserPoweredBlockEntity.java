package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityCondenserPowered} (samt {@code TileEntityCondenser.updateEntity}): kondensiert Abdampf zu
 * Wasser, soviel Eingang und freier Ausgang erlauben ({@code convert}), sofern mindestens {@code convert * 10} HE da
 * sind; verbraucht {@code convert * powerConsumption}. Nach jeder Umwandlung laufen die Luefter eine Sekunde nach und
 * blasen Wolken seitlich aus. Sechs Anschluesse auf Hoehe 1 (Fluid und Strom).
 */
public class MachineCondenserPoweredBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    /* KONFIGURIERBAR (Original IConfigurableMachine "condenserPowered") */
    public static long maxPower = 10_000_000;
    public static int inputTankSizeP = 1_000_000;
    public static int outputTankSizeP = 1_000_000;
    public static int powerConsumption = 10;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), inputTankSizeP),
            new FluidTank(ModFluids.WATER.getSource(), outputTankSizeP)
    };

    public int age = 0;
    public int waterTimer = 0;
    protected int throughput;

    public float spin;
    public float lastSpin;

    public MachineCondenserPoweredBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSER_POWERED_BE.get(), pos, state, 0, maxPower, maxPower, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCondenserPoweredBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick(level, pos);
    }

    private Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    private void serverTick(ServerLevel world) {

        age++;
        if (age >= 2) {
            age = 0;
        }

        if (this.waterTimer > 0)
            this.waterTimer--;

        int convert = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
        this.throughput = convert;

        if (extraCondition(convert)) {
            tanks[0].setFill(tanks[0].getFill() - convert);

            if (convert > 0)
                this.waterTimer = 20;

            tanks[1].setFill(tanks[1].getFill() + convert);
            postConvert(convert);
        }

        for (DirPos con : getConPos()) {
            this.trySubscribe(this.tanks[0].getTankType(), world, con.pos, con.dir);
            this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
        }
        for (DirPos con : getConPos()) {
            this.tryProvide(this.tanks[1], world, con.pos, con.dir);
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level world, BlockPos pos) {

        this.lastSpin = this.spin;

        if (this.waterTimer > 0) {
            this.spin += 30F;

            if (this.spin >= 360F) {
                this.spin -= 360F;
                this.lastSpin -= 360F;
            }

            if (world.getGameTime() % 4 == 0) {
                Direction dir = dir();
                world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + dir.getStepX() * 1.5, pos.getY() + 1.5, pos.getZ() + 0.5 + dir.getStepZ() * 1.5, dir.getStepX() * 0.1, 0, dir.getStepZ() * 0.1);
                world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 - dir.getStepX() * 1.5, pos.getY() + 1.5, pos.getZ() + 0.5 - dir.getStepZ() * 1.5, dir.getStepX() * -0.1, 0, dir.getStepZ() * -0.1);
            }
        }
    }

    public boolean extraCondition(int convert) {
        return energy >= convert * 10L;
    }

    public void postConvert(int convert) {
        this.energy -= (long) convert * powerConsumption;
        if (this.energy < 0) this.energy = 0;
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    public DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)
        BlockPos p = worldPosition.above();

        return new DirPos[] {
                new DirPos(p.relative(rot, 4), rot),
                new DirPos(p.relative(rot, -4), rot.getOpposite()),
                new DirPos(p.relative(dir, 2).relative(rot, -1), dir),
                new DirPos(p.relative(dir, 2).relative(rot, 1), dir),
                new DirPos(p.relative(dir, -2).relative(rot, -1), dir.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot, 1), dir.getOpposite())
        };
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public int getThroughput() { return throughput; }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
        tag.putByte("waterTimer", (byte) waterTimer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
        waterTimer = tag.getByte("waterTimer");
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.condenser_powered");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 3, worldPosition.getZ() + 4);
        return bb;
    }
}
