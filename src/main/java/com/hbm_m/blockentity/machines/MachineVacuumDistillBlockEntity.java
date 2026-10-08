package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineVacuumDistillMenu;
import com.hbm_m.recipe.VacuumDistillRecipe;

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
 * 1:1 {@code TileEntityMachineVacuumDistill}: 12 Slots (Batterie, zwei stillgelegte Kanisterplaetze, je Ausgang ein
 * Paar Fuell-/Ausgabeplaetze, Fluidkennung), Eingangstank 64.000 mB unter Druck 2, vier Ausgangstanks je 24.000 mB.
 * Jeden Tick 100 mB Eingang + 10.000 HE -> vier Fraktionen ({@code VacuumRefineryRecipes}). Strom und Fluide an acht
 * Anschluessen; Kochgeraeusch, solange sie arbeitet.
 */
public class MachineVacuumDistillBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COUNT = 12;

    public static final long maxPower = 1_000_000;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.CRUDE_OIL.getSource(), 64_000).withPressure(2),
            new FluidTank(ModFluids.HEAVYOIL_VACUUM.getSource(), 24_000),
            new FluidTank(ModFluids.REFORMATE.getSource(), 24_000),
            new FluidTank(ModFluids.LIGHTOIL_VACUUM.getSource(), 24_000),
            new FluidTank(ModFluids.SOURGAS.getSource(), 24_000)
    };

    public boolean isOn;
    private int audioTime;

    public MachineVacuumDistillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUM_DISTILL_BE.get(), pos, state, SLOT_COUNT, maxPower, maxPower, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineVacuumDistillBlockEntity be) {
        if (level instanceof ServerLevel world) be.serverTick(world, pos);
        else be.clientTick();
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.offset(2, 0, 1), Direction.EAST),
                new DirPos(p.offset(2, 0, -1), Direction.EAST),
                new DirPos(p.offset(-2, 0, 1), Direction.WEST),
                new DirPos(p.offset(-2, 0, -1), Direction.WEST),
                new DirPos(p.offset(1, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(-1, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(1, 0, -2), Direction.NORTH),
                new DirPos(p.offset(-1, 0, -2), Direction.NORTH)
        };
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        this.isOn = false;

        for (DirPos con : getConPos()) {
            this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            this.trySubscribe(tanks[0].getTankType(), world, con.pos, con.dir);
        }
        chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = inventory.getStackInSlot(i);
        boolean changed = tanks[0].setType(11, slots);
        changed |= tanks[0].loadTank(1, 2, slots);

        refine();

        changed |= tanks[1].unloadTank(3, 4, slots);
        changed |= tanks[2].unloadTank(5, 6, slots);
        changed |= tanks[3].unloadTank(7, 8, slots);
        changed |= tanks[4].unloadTank(9, 10, slots);
        if (changed) for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);

        for (DirPos con : getConPos()) {
            for (int i = 1; i < 5; i++) {
                if (tanks[i].getFill() > 0) this.tryProvide(tanks[i], world, con.pos, con.dir);
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        if (this.isOn) audioTime = 20;
        boolean play = audioTime > 0;
        if (audioTime > 0) audioTime--;
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, play, this::createAudioLoop);
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.VacuumDistillLoopSoundFactory").getMethod("create", MachineVacuumDistillBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
    }

    /** Fuer den Loop-Sound: laeuft 20 Ticks nach dem letzten Arbeitstick nach. */
    public boolean isAudible() {
        return audioTime > 0 || isOn;
    }

    private void refine() {
        VacuumDistillRecipe refinery = VacuumDistillRecipe.getRecipe(level, tanks[0].getTankType());
        if (refinery == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(ModFluids.NONE.getSource());
            return;
        }

        Fluid[] types = { refinery.getHeavy(), refinery.getReformate(), refinery.getLight(), refinery.getSour() };
        int[] fills = { refinery.getHeavyMb(), refinery.getReformateMb(), refinery.getLightMb(), refinery.getSourMb() };
        for (int i = 0; i < 4; i++) tanks[i + 1].setTankType(types[i]);

        if (energy < 10_000) return;
        if (tanks[0].getFill() < 100) return;
        for (int i = 0; i < 4; i++) if (tanks[i + 1].getFill() + fills[i] > tanks[i + 1].getMaxFill()) return;

        this.isOn = true;
        energy -= 10_000;
        tanks[0].setFill(tanks[0].getFill() - 100);

        for (int i = 0; i < 4; i++) tanks[i + 1].setFill(tanks[i + 1].getFill() + fills[i]);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != null && side != Direction.DOWN;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2], tanks[3], tanks[4] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fromDir != Direction.DOWN;
    }

    public boolean isOn() { return isOn; }
    public FluidTank[] getTanks() { return tanks; }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("power", energy);
        tanks[0].writeToNBT(tag, "input");
        tanks[1].writeToNBT(tag, "heavy");
        tanks[2].writeToNBT(tag, "reformate");
        tanks[3].writeToNBT(tag, "light");
        tanks[4].writeToNBT(tag, "gas");
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        energy = tag.getLong("power");
        tanks[0].readFromNBT(tag, "input");
        tanks[1].readFromNBT(tag, "heavy");
        tanks[2].readFromNBT(tag, "reformate");
        tanks[3].readFromNBT(tag, "light");
        tanks[4].readFromNBT(tag, "gas");
        isOn = tag.getBoolean("isOn");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.vacuum_distill");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == 11) return true;
        // Original: Platz 1/2 stillgelegt (Kanister brauchen Druck), Ausgaben nur entnehmbar
        return slot == 3 || slot == 5 || slot == 7 || slot == 9;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineVacuumDistillMenu.create(id, inventory, this);
    }

    /** Original: 3x9x3 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 9, worldPosition.getZ() + 2);
    }

    /** Original {@code TileEntityMachineVacuumDistill.writeNBT}: die fuenf Tanks, sofern einer etwas enthaelt. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = true;
        for (var tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        tanks[0].writeToNBT(nbt, "input");
        tanks[1].writeToNBT(nbt, "heavy");
        tanks[2].writeToNBT(nbt, "reformate");
        tanks[3].writeToNBT(nbt, "light");
        tanks[4].writeToNBT(nbt, "gas");
    }
}
