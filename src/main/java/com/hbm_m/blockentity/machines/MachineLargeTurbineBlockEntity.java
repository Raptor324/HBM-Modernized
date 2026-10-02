package com.hbm_m.blockentity.machines;

import java.util.Random;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.inventory.menu.MachineLargeTurbineMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineLargeTurbine} (im Original "LEGACY"): entspannt Dampf nach {@link FT_Coolable}
 * (Turbine), hoechstens ein Fuenftel des Tankinhalts je Tick; der Energiepuffer verliert jeden Tick 5 %. Dampf rein
 * und Abdampf raus seitlich und vorn, Strom hinten. Der Rotor laeuft auf dem Client mit Beschleunigung hoch und aus.
 */
public class MachineLargeTurbineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_FLUID_ID_IN    = 0;
    public static final int SLOT_FLUID_ID_OUT   = 1;
    public static final int SLOT_INPUT_IO_IN    = 2;
    public static final int SLOT_INPUT_IO_OUT   = 3;
    public static final int SLOT_BATTERY        = 4;
    public static final int SLOT_OUTPUT_IO_IN   = 5;
    public static final int SLOT_OUTPUT_IO_OUT  = 6;
    public static final int INVENTORY_SIZE      = 7;

    /* KONFIGURIERBAR (Original IConfigurableMachine) */
    public static long maxPower = 100000000;
    public static int inputTankSize = 512_000;
    public static int outputTankSize = 10_240_000;
    public static double efficiency = 1.0;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.STEAM.getSource(), inputTankSize),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), outputTankSize)
    };

    public double[] info = new double[3];
    private boolean operational;
    private boolean shouldTurn;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration = 0F;
    private final float audioDesync;

    public MachineLargeTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_TURBINE_BE.get(), pos, state, INVENTORY_SIZE, maxPower, 0L, maxPower);
        this.audioDesync = new Random().nextFloat() * 0.05F;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineLargeTurbineBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick();
    }

    private Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    private void serverTick(ServerLevel world) {

        this.info = new double[3];

        Direction dir = dir();
        BlockPos back = worldPosition.relative(dir, -4);
        this.tryProvide(world, back.getX(), back.getY(), back.getZ(), dir.getOpposite());
        for (DirPos con : getConPos()) this.trySubscribe(tanks[0].getTankType(), world, con.pos, con.dir);
        for (DirPos con : getConPos()) this.tryProvide(tanks[1], world, con.pos, con.dir);

        ItemStack[] slots = slotsArray();
        boolean changed = tanks[0].setType(SLOT_FLUID_ID_IN, SLOT_FLUID_ID_OUT, slots);
        changed |= tanks[0].loadTank(SLOT_INPUT_IO_IN, SLOT_INPUT_IO_OUT, slots);
        if (changed) applySlots(slots);
        // Original: Library.chargeItemsFromTE(slots, 4, power, maxPower)
        chargeItemInSlot(SLOT_BATTERY);

        this.energy = (long) (this.energy * 0.95);

        Fluid in = tanks[0].getTankType();
        boolean valid = false;
        FT_Coolable trait = FluidType.getTrait(in, FT_Coolable.class);
        if (trait != null) {
            double eff = trait.getEfficiency(CoolingType.TURBINE) * efficiency; // Standard: 100 %
            if (eff > 0) {
                tanks[1].setTankType(trait.coolsTo);
                int inputOps = (int) Math.floor(tanks[0].getFill() / trait.amountReq); // Zyklen mit dem ganzen Eingang
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced; // Zyklen mit dem freien Ausgang
                int cap = (int) Math.ceil(tanks[0].getFill() / trait.amountReq / 5F); // "hoechstens 20 %"-Regel
                int ops = Math.min(inputOps, Math.min(outputOps, cap));
                tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
                tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
                this.energy += (ops * trait.heatEnergy * eff);
                info[0] = ops * trait.amountReq;
                info[1] = ops * trait.amountProduced;
                info[2] = ops * trait.heatEnergy * eff;
                valid = true;
                operational = ops > 0;
            }
        }
        if (!valid) tanks[1].setTankType(ModFluids.NONE.getSource());
        if (energy > maxPower) energy = maxPower;

        slots = slotsArray();
        if (tanks[1].unloadTank(SLOT_OUTPUT_IO_IN, SLOT_OUTPUT_IO_OUT, slots)) applySlots(slots);

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;

        if (this.rotor >= 360) {
            this.rotor -= 360;
            this.lastRotor -= 360;
        }

        if (shouldTurn) {
            // Original: zufaelliger Versatz, damit sich mehrere Turbinen nicht gleich anhoeren
            this.fanAcceleration = Math.max(0F, Math.min(15F, this.fanAcceleration += 0.075F + audioDesync));
        } else {
            this.fanAcceleration = Math.max(0F, Math.min(15F, this.fanAcceleration -= 0.1F));
        }

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, shouldTurn || fanAcceleration > 0, this::createAudioLoop);
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.LargeTurbineLoopSoundFactory").getMethod("create", MachineLargeTurbineBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    protected DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)
        return new DirPos[] {
                new DirPos(worldPosition.relative(rot, 2), rot),
                new DirPos(worldPosition.relative(rot, -2), rot.getOpposite()),
                new DirPos(worldPosition.relative(dir, 2), dir)
        };
    }

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    // ── Fluid ───────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── Zugriff ────────────────────────────────────────────────────────────

    public FluidTank[] getTanks() { return tanks; }

    public int getPowerScaled(int i) {
        return (int) ((energy * i) / maxPower);
    }

    public boolean isActive() { return shouldTurn; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
        tag.putBoolean("operational", operational);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
        operational = tag.getBoolean("operational");
    }

    /** Original {@code deserialize}: {@code shouldTurn} ist das vom Server gemeldete {@code operational}. */
    @Override
    protected void applyClientUpdate(CompoundTag tag) {
        readNbtData(tag, null);
        shouldTurn = operational;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    // ── Menue ───────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLargeTurbine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineLargeTurbineMenu.create(id, inventory, this);
    }

    /** Original: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
