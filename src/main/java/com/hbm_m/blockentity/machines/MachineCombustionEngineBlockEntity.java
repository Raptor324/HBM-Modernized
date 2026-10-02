package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.menu.MachineCombustionEngineMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineCombustionEngine}: Verbrennungsmotor fuer jedes {@link FT_Combustible}-Fluid. Mit
 * Zuendung und Drossel ({@code setting}, 0-30) verbrennt er {@code setting * 2} Zehntel-mB je Tick; der Kolbensatz
 * bestimmt je Treibstoffgrad den Wirkungsgrad (Stahl, Dura, Desh, Sternmetall). Abgas geht in die Rauchtanks und ueber
 * die vier Anschluesse hinaus; Strom geht dort ebenfalls hinaus. Die Wartungsklappe oeffnet sich, solange jemand die
 * GUI offen hat.
 */
public class MachineCombustionEngineBlockEntity extends com.hbm_m.blockentity.MachinePollutingBlockEntity
        implements IFluidStandardTransceiverMK2,
                   com.hbm_m.api.redstoneoverradio.IRORValueProvider,
                   com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_FLUID_OUT = 1;
    public static final int SLOT_PISTON = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    private static final int SLOT_COUNT = 5;

    public static final int MAX_THROTTLE = 30;
    public static final long maxPower = 2_500_000;

    /** Original {@code EnumPistonType.eff}: Wirkungsgrad je Treibstoffgrad (LOW, MEDIUM, HIGH, AVIATION, GASEOUS). */
    public static final double[][] PISTON_EFF = {
            { 1.00, 0.75, 0.25, 0.00, 0.00 }, // STEEL
            { 0.50, 1.00, 0.90, 0.50, 0.00 }, // DURA
            { 0.00, 0.50, 1.00, 0.75, 0.00 }, // DESH
            { 0.50, 0.75, 1.00, 0.90, 0.50 }, // STARMETAL
    };

    public boolean isOn = false;
    private int playersUsing = 0;
    public int setting = 0;
    public boolean wasOn = false;

    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    public final FluidTank tank = new FluidTank(ModFluids.DIESEL.getSource(), 24_000);
    public int tenth = 0;

    public MachineCombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        // Original: super(5, 50) - fuenf Slots, 50 mB Rauchpuffer je Sorte.
        super(ModBlockEntities.COMBUSTION_ENGINE_BE.get(), pos, state, SLOT_COUNT, maxPower, 0L, maxPower, 50);
    }

    /** Original {@code EnumPistonType}-Ordinal des Kolbensatzes, -1 ohne. */
    public static int pistonType(Item piston) {
        if (piston == ModItems.PISTON_SET_STEEL.get()) return 0;
        if (piston == ModItems.PISTON_SET_DURA.get()) return 1;
        if (piston == ModItems.PISTON_SET_DESH.get()) return 2;
        if (piston == ModItems.PISTON_SET_STARMETAL.get()) return 3;
        return -1;
    }

    /** Wirkungsgrad des eingesetzten Kolbensatzes fuer das Fluid, 0 ohne Kolben oder ohne Verbrennbarkeit. */
    public static double efficiency(ItemStack piston, Fluid fluid) {
        int type = pistonType(piston.getItem());
        FT_Combustible trait = FluidType.getTrait(fluid, FT_Combustible.class);
        if (type < 0 || trait == null) return 0;
        int grade = trait.getGrade().ordinal();
        return grade < PISTON_EFF[type].length ? PISTON_EFF[type][grade] : 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCombustionEngineBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world) {

        ItemStack[] slots = slotsArray();
        boolean changed = this.tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, slots);
        if (this.tank.setType(SLOT_FLUID_ID, slots)) {
            this.tenth = 0;
            changed = true;
        }
        if (changed) applySlots(slots);

        wasOn = false;

        int fill = tank.getFill() * 10 + tenth;
        ItemStack piston = inventory.getStackInSlot(SLOT_PISTON);
        FT_Combustible trait = FluidType.getTrait(tank.getTankType(), FT_Combustible.class);
        if (isOn && setting > 0 && pistonType(piston.getItem()) >= 0 && fill > 0 && trait != null) {

            double eff = efficiency(piston, tank.getTankType());

            if (eff > 0) {
                int speed = setting * 2;

                int toBurn = Math.min(fill, speed);
                this.energy += toBurn * (trait.getCombustionEnergy() / 10_000D) * eff;
                fill -= toBurn;

                if (world.getGameTime() % 5 == 0 && toBurn > 0) {
                    super.pollute(tank.getTankType(), FluidReleaseType.BURN, toBurn * 0.5F);
                }

                if (toBurn > 0) {
                    wasOn = true;
                }

                tank.setFill(fill / 10);
                tenth = fill % 10;
            }
        }

        // Original: Library.chargeItemsFromTE(slots, 3, power, power)
        chargeItemInSlot(SLOT_BATTERY);

        for (DirPos con : getConPos()) {
            this.tryProvide(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            this.trySubscribe(tank.getTankType(), world, con.pos, con.dir);
            this.sendSmoke(world, con.pos, con.dir);
        }

        if (energy > maxPower)
            energy = maxPower;

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = (doorAngle / 10F) + 3;

        if (this.playersUsing > 0) {
            this.doorAngle += swingSpeed;
        } else {
            this.doorAngle -= swingSpeed;
        }

        this.doorAngle = Mth.clamp(this.doorAngle, 0F, 135F);

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, wasOn, this::createAudioLoop);
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.CombustionEngineLoopSoundFactory").getMethod("create", MachineCombustionEngineBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        BlockState state = getBlockState();
        Direction dir = state.hasProperty(com.hbm_m.block.machines.MachineCombustionEngineBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineCombustionEngineBlock.FACING) : Direction.NORTH;
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)
        BlockPos p = worldPosition;

        return new DirPos[] {
                new DirPos(p.relative(dir, 1).relative(rot, 1), dir),
                new DirPos(p.relative(dir, 1).relative(rot, -1), dir),
                new DirPos(p.relative(dir, -2).relative(rot, 1), dir.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot, -1), dir.getOpposite())
        };
    }

    /** Original {@code openInventory}/{@code closeInventory} - fuer die Wartungsklappe. */
    public void openInventory() { if (level != null && !level.isClientSide) this.playersUsing++; }
    public void closeInventory() { if (level != null && !level.isClientSide) this.playersUsing--; }

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    // ── Redstone-over-Radio ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "throttle",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "efficiency",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "throttle"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "state").equals(name))      return "" + (isOn ? 1 : 0);
        if ((PREFIX_VALUE + "throttle").equals(name))   return "" + setting;
        if ((PREFIX_VALUE + "power").equals(name))      return "" + energy;
        if ((PREFIX_VALUE + "fuel").equals(name))       return "" + tank.getFill();
        if ((PREFIX_VALUE + "efficiency").equals(name)) {
            ItemStack piston = inventory.getStackInSlot(SLOT_PISTON);
            if (pistonType(piston.getItem()) >= 0 && FluidType.getTrait(tank.getTankType(), FT_Combustible.class) != null) {
                return "" + (int) Math.round(efficiency(piston, tank.getTankType()) * 100);
            }
            return "0";
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            try {
                int val = Integer.parseInt(params[0]);
                this.isOn = (val == 1);
                this.setChanged();
            } catch (NumberFormatException e) { }
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            try {
                int val = Integer.parseInt(params[0]);
                if (val < 0) val = 0;
                if (val > 30) val = 30;
                this.setting = val;
                this.setChanged();
            } catch (NumberFormatException e) { }
            return null;
        }
        return null;
    }

    public boolean isOn()      { return isOn; }
    public int getSetting()    { return setting; }

    /** Original {@code receiveControl("turnOn")}. */
    public void toggleIgnition() {
        isOn = !isOn;
        setChanged();
        sendUpdateToClient();
    }

    /** Original {@code receiveControl("setting")}. */
    public void setThrottle(int throttle) {
        setting = throttle;
        setChanged();
        sendUpdateToClient();
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getSendingTanks() { return this.getSmokeTanks(); }

    /** Original: kein Anschluss von unten. */
    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != Direction.DOWN;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != Direction.DOWN;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("setting", setting);
        tag.putBoolean("isOn", isOn);
        tank.writeToNBT(tag, "tank");
        tag.putInt("tenth", tenth);
        tag.putInt("playersUsing", playersUsing);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        setting = tag.getInt("setting");
        isOn = tag.getBoolean("isOn");
        tank.readFromNBT(tag, "tank");
        tenth = tag.getInt("tenth");
        playersUsing = tag.getInt("playersUsing");
        wasOn = tag.getBoolean("wasOn");
    }

    // ==================== Menue ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.combustionEngine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    /** Original {@code TileEntityMachineBase.isItemValidForSlot}: nichts per Automatisierung. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCombustionEngineMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 2, worldPosition.getZ() + 4);
        return bb;
    }
}
