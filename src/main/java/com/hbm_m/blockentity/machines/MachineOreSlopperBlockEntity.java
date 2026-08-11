package com.hbm_m.blockentity.machines;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineOreSlopperMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.worldgen.BedrockOreDensity;
import com.hbm_m.platform.PlatformHooks;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ore Slopper - Einzelblock-Maschine, Portierung der Kernrezeptlogik aus
 * {@code TileEntityMachineOreSlopper} (1.7.10-Original): Bedrock-Erz-Item ({@code bedrock_ore_base},
 * mit den 6 Kategorie-Dichten als NBT getaggt, siehe {@link MachineMiningDrillBlockEntity#mineBedrockOre})
 * wird mit Wasser "geschlaemmt" und liefert nach und nach die 6 sortierten Basis-Erze
 * ({@code bedrock_ore_base_<type>}).
 * <p>
 * Bewusst NICHT uebernommen (siehe Aufgabenstellung): SLOP-Fluid-Nebenprodukt, "toetet Entitaeten
 * obendrauf"-Mechanik. Energieverbrauch (200/Tick, siehe {@code TileEntityMachineOreSlopper.consumptionBase})
 * jetzt 1:1 uebernommen ueber einen Batterie-Slot, analog {@link MachineMiningDrillBlockEntity}.
 */
public class MachineOreSlopperBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    private static final int OUTPUT_START = 1;
    public static final int SLOT_BATTERY = 7;
    private static final int SLOT_COUNT = 8; // 1 Eingang + 6 Ausgaenge + 1 Batterie

    public static final int CYCLE_TICKS = 400;
    private static final int WATER_PER_CYCLE_MB = 1000;
    private static final int TANK_CAPACITY_MB = 4000;

    private static final long CAPACITY = 100_000L;
    private static final long MAX_RECEIVE = 1_000L;
    private static final long ENERGY_PER_TICK = 200L;

    private int progressTicks = 0;
    private boolean active = false;
    private final double[] ores = new double[BedrockOreDensity.Type.values().length];

    /** Client-seitige Animation (siehe MachineOreSlopperRenderer): Fan- und Blades-Rotation, 1:1
     *  Pivot-/Achsenwerte aus {@code RenderOreSlopper} (Original), Rotationsgeschwindigkeit selbst
     *  gewaehlt (Original haelt keinen festen Increment-Wert, nur prev/current-Interpolation). */
    public float fanRotation, prevFanRotation;
    public float bladesRotation, prevBladesRotation;

    private final FluidTank tank = new FluidTank(ModFluids.WATER.getSource(), TANK_CAPACITY_MB);

    public MachineOreSlopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_SLOPPER_BE.get(), pos, state, SLOT_COUNT, CAPACITY, MAX_RECEIVE, 0L);
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(tank);
    }
    //?}

    // ==================== OUTPUT ITEM MAPPING ====================

    private static RegistrySupplier<Item> outputItemSupplier(BedrockOreDensity.Type type) {
        return switch (type) {
            case LIGHT -> ModItems.BEDROCK_ORE_BASE_LIGHT;
            case HEAVY -> ModItems.BEDROCK_ORE_BASE_HEAVY;
            case RARE -> ModItems.BEDROCK_ORE_BASE_RARE;
            case ACTINIDE -> ModItems.BEDROCK_ORE_BASE_ACTINIDE;
            case NONMETAL -> ModItems.BEDROCK_ORE_BASE_NONMETAL;
            case CRYSTAL -> ModItems.BEDROCK_ORE_BASE_CRYSTAL;
        };
    }

    private static Item outputItem(int ordinal) {
        return outputItemSupplier(BedrockOreDensity.Type.values()[ordinal]).get();
    }

    // ==================== TICK ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineOreSlopperBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick();
        } else {
            be.serverTick();
        }
    }

    private void clientTick() {
        prevFanRotation = fanRotation;
        prevBladesRotation = bladesRotation;
        if (active) {
            fanRotation += 10F;
            bladesRotation += 10F;
            if (fanRotation >= 360F) {
                fanRotation -= 360F;
                prevFanRotation -= 360F;
            }
            if (bladesRotation >= 360F) {
                bladesRotation -= 360F;
                prevBladesRotation -= 360F;
            }
        }
    }

    private void serverTick() {
        boolean dirty = false;

        chargeFromBatterySlot(SLOT_BATTERY);

        boolean wasActive = active;
        active = canSlop();
        if (active != wasActive) {
            dirty = true;
        }

        if (active) {
            setEnergyStored(getEnergyStored() - ENERGY_PER_TICK);
            progressTicks++;
            dirty = true;
            if (progressTicks >= CYCLE_TICKS) {
                progressTicks = 0;
                completeCycle();
            }
        } else if (progressTicks > 0) {
            progressTicks = 0;
            dirty = true;
        }

        if (flushOres()) {
            dirty = true;
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean canSlop() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;
        if (tank.getFluidAmountMb() < WATER_PER_CYCLE_MB) return false;
        if (getEnergyStored() < ENERGY_PER_TICK) return false;
        return hasAnyOutputSpace();
    }

    private boolean hasAnyOutputSpace() {
        for (int t = 0; t < ores.length; t++) {
            ItemStack sim = inventory.insertItem(OUTPUT_START + t, new ItemStack(outputItem(t), 1), true);
            if (sim.isEmpty()) return true;
        }
        return false;
    }

    private void completeCycle() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return;

        // Dichten VOR dem Verbrauch des Items auslesen (auch wenn der Stack danach leer wird).
        CompoundTag nbt = PlatformHooks.getItemTag(input);
        double[] gained = new double[ores.length];
        if (nbt != null) {
            for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
                String key = type.name().toLowerCase(Locale.ROOT);
                if (nbt.contains(key)) {
                    gained[type.ordinal()] = nbt.getDouble(key);
                }
            }
        }

        inventory.extractItem(SLOT_INPUT, 1, false);
        tank.drainMb(WATER_PER_CYCLE_MB);

        for (int i = 0; i < ores.length; i++) {
            ores[i] += gained[i];
        }
    }

    /** @return true wenn sich mindestens ein Ausgabe-Slot geaendert hat. */
    private boolean flushOres() {
        boolean changed = false;
        for (int t = 0; t < ores.length; t++) {
            while (ores[t] >= 1.0D) {
                ItemStack toInsert = new ItemStack(outputItem(t), 1);
                ItemStack leftover = inventory.insertItem(OUTPUT_START + t, toInsert, false);
                if (leftover.isEmpty()) {
                    ores[t] -= 1.0D;
                    changed = true;
                } else {
                    break; // kein Platz - Rest bleibt fuer den naechsten Tick akkumuliert.
                }
            }
        }
        return changed;
    }

    // ==================== NBT ====================

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progressTicks);
        tag.putBoolean("active", active);
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            tag.putDouble("ores_" + type.name().toLowerCase(Locale.ROOT), ores[type.ordinal()]);
        }
        tank.writeToNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putInt("progress", progressTicks);
        tag.putBoolean("active", active);
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            tag.putDouble("ores_" + type.name().toLowerCase(Locale.ROOT), ores[type.ordinal()]);
        }
        tank.writeToNBT(tag, "tank");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progressTicks = tag.getInt("progress");
        active = tag.getBoolean("active");
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            String key = "ores_" + type.name().toLowerCase(Locale.ROOT);
            ores[type.ordinal()] = tag.contains(key) ? tag.getDouble(key) : 0.0D;
        }
        tank.readFromNBT(tag, "tank");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        progressTicks = tag.getInt("progress");
        active = tag.getBoolean("active");
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            String key = "ores_" + type.name().toLowerCase(Locale.ROOT);
            ores[type.ordinal()] = tag.contains(key) ? tag.getDouble(key) : 0.0D;
        }
        tank.readFromNBT(tag, "tank");
    
    }
    *///?}

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.ore_slopper");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            return stack.getItem() == ModItems.BEDROCK_ORE_BASE.get();
        }
        if (slot == SLOT_BATTERY) {
            return isEnergyProviderItem(stack);
        }
        int outIdx = slot - OUTPUT_START;
        if (outIdx >= 0 && outIdx < ores.length) {
            return stack.getItem() == outputItem(outIdx);
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineOreSlopperMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    public int getProgress() {
        return progressTicks;
    }

    public int getMaxProgress() {
        return CYCLE_TICKS;
    }

    public int getProgressScaled(int scale) {
        return (progressTicks * scale) / CYCLE_TICKS;
    }
}
