package com.hbm_m.blockentity.machines;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineReactorResearchMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemPlateFuel;
import com.hbm_m.radiation.ChunkRadiationManager;

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
import net.minecraft.tags.FluidTags;

/**
 * Research Reactor - Port von {@code TileEntityReactorResearch} (1.7.10 Original, dort selbst als
 * "TODO: fix reactor control" markiert). 12 Brennstoffplatten-Slots in einem festen
 * Nachbarschafts-Graphen (1:1 aus {@code getNeighboringSlots}), Neutronenfluss diffundiert jeden
 * Tick zu den Nachbarslots skaliert mit {@code level} (0-1, "Regelstab-Tiefe"). Kuehlwasser wird
 * direkt im Level ueber Wasserbloecke an festen Offsets erkannt (kein Fluid-Tank, 1:1 aus {@code
 * getWater}). Bei Ueberhitzung ({@code heat > maxHeat}) Explosion + Strahlungsausstoss.
 * <p>
 * SCOPE-Entscheidungen (dokumentierte Luecken):
 * <ul>
 *   <li>Die manuelle GUI-Schieberegler-Steuerung ({@code IControlReceiver}, Maus-Drag sendet NBT-
 *   Paket) entfaellt zugunsten von Redstone-Sperre (entsperrt = {@code targetLevel=1.0}, gesperrt
 *   = {@code targetLevel=0}) - analog zu {@code MachineCombustionEngineBlockEntity}.</li>
 *   <li>Die Corium-Block-Platzierung nach einer Explosion entfaellt (kein Corium-Block in diesem
 *   Port vorhanden) - Explosion und Strahlungsausstoss selbst sind 1:1 uebernommen.</li>
 *   <li>Das Meteoritenschwert-Bestrahlungs-Easter-Egg und die BossSpawnHandler-FBI-Markierung
 *   entfallen (fehlende Items/Infrastruktur).</li>
 * </ul>
 */
public class MachineReactorResearchBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 12;
    private static final int MAX_HEAT = 50_000;
    private static final double SPEED = 0.04D;

    private static final Map<Item, Item> FUEL_TO_WASTE = new HashMap<>();
    static {
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_U233.get(), ModItems.WASTE_PLATE_U233.get());
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_U235.get(), ModItems.WASTE_PLATE_U235.get());
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_MOX.get(), ModItems.WASTE_PLATE_MOX.get());
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_PU239.get(), ModItems.WASTE_PLATE_PU239.get());
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_SA326.get(), ModItems.WASTE_PLATE_SA326.get());
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_RA226BE.get(), ModItems.WASTE_PLATE_RA226BE.get());
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_PU238BE.get(), ModItems.WASTE_PLATE_PU238BE.get());
    }

    private int heat;
    private int water;
    private double level;
    private double targetLevel;
    private final int[] slotFlux = new int[INVENTORY_SIZE];
    private int totalFlux;
    private boolean exploded;

    public MachineReactorResearchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_RESEARCH_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineReactorResearchBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        if (exploded) return;

        rodControl(level, pos);

        totalFlux = 0;
        if (this.level > 0) {
            reaction(level);
        }

        if (heat > 0) {
            water = getWater(level, pos);
            if (water > 0) {
                heat -= (int) (heat * 0.07F * water / 12);
            } else {
                heat -= 1;
            }
            if (heat < 0) heat = 0;
        }

        if (heat > MAX_HEAT) {
            explode(level, pos);
            return;
        }

        if (this.level > 0 && heat > 0) {
            float rad = (float) heat / MAX_HEAT * 50F;
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), rad);
        }

        setChanged();
        sendUpdateToClient();
    }

    /** Redstone-Sperre statt manuellem Schieberegler (siehe Klassenkommentar). */
    private void rodControl(Level level, BlockPos pos) {
        targetLevel = level.hasNeighborSignal(pos) ? 0D : 1.0D;

        if (this.level < targetLevel) {
            this.level = Math.min(this.level + SPEED, targetLevel);
        } else if (this.level > targetLevel) {
            this.level = Math.max(this.level - SPEED, targetLevel);
        }
    }

    private void reaction(Level level) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                slotFlux[i] = 0;
                continue;
            }

            if (stack.getItem() instanceof ItemPlateFuel rod) {
                int outFlux = rod.react(level, stack, slotFlux[i]);
                heat += outFlux * 2;
                slotFlux[i] = 0;
                totalFlux += outFlux;

                if (ItemPlateFuel.getLifeTime(stack) > rod.lifeTime) {
                    Item waste = FUEL_TO_WASTE.get(stack.getItem());
                    inventory.setStackInSlot(i, waste != null ? new ItemStack(waste, 1) : ItemStack.EMPTY);
                }

                for (int neighbor : getNeighboringSlots(i)) {
                    slotFlux[neighbor] += (int) (outFlux * this.level);
                }
                continue;
            }

            slotFlux[i] = 0;
        }
    }

    private static int[] getNeighboringSlots(int id) {
        return switch (id) {
            case 0 -> new int[]{1, 5};
            case 1 -> new int[]{0, 6};
            case 2 -> new int[]{3, 7};
            case 3 -> new int[]{2, 4, 8};
            case 4 -> new int[]{3, 9};
            case 5 -> new int[]{0, 6, 10};
            case 6 -> new int[]{1, 5, 11};
            case 7 -> new int[]{2, 8};
            case 8 -> new int[]{3, 7, 9};
            case 9 -> new int[]{4, 8};
            case 10 -> new int[]{5, 11};
            case 11 -> new int[]{6, 10};
            default -> new int[0];
        };
    }

    /** 1:1 aus dem Original ({@code getWater}) - direkte Wasserblock-Erkennung, kein Fluid-Tank. */
    private static int getWater(Level level, BlockPos pos) {
        int water = 0;
        water += isWater(level, pos.above(3)) ? 1 : 0;
        water += isWater(level, pos.below(1)) ? 1 : 0;
        for (int i = 0; i < 3; i++) {
            water += isWater(level, pos.north().above(i)) ? 1 : 0;
            water += isWater(level, pos.south().above(i)) ? 1 : 0;
            water += isWater(level, pos.east().above(i)) ? 1 : 0;
            water += isWater(level, pos.west().above(i)) ? 1 : 0;
        }
        return water;
    }

    private static boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private void explode(Level level, BlockPos pos) {
        exploded = true;
        dropInventoryContents();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18.0F, Level.ExplosionInteraction.BLOCK);
        ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), 50);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int getHeat()          { return heat; }
    public int getMaxHeat()       { return MAX_HEAT; }
    public int getWater()         { return water; }
    public double getRodLevel()   { return level; }
    public double getTargetLevel() { return targetLevel; }
    public int getTotalFlux()     { return totalFlux; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", heat);
        tag.putInt("water", water);
        tag.putDouble("level", level);
        tag.putDouble("target_level", targetLevel);
        tag.putBoolean("exploded", exploded);
        tag.putIntArray("slot_flux", slotFlux);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.getInt("heat");
        water = tag.getInt("water");
        level = tag.getDouble("level");
        targetLevel = tag.getDouble("target_level");
        exploded = tag.getBoolean("exploded");
        int[] flux = tag.getIntArray("slot_flux");
        for (int i = 0; i < Math.min(flux.length, INVENTORY_SIZE); i++) slotFlux[i] = flux[i];
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemPlateFuel;
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.reactor_research");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineReactorResearchMenu.create(id, inventory, this);
    }
}
