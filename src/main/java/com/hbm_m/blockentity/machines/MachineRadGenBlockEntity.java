package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineRadGenMenu;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.RadGenRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * RadGen - Port von {@code TileEntityMachineRadGen} (1.7.10 Original). 12 parallele
 * Verarbeitungs-"Warteschlangen" (Slots 0-11 Eingabe, 12-23 Ausgabe), jede verarbeitet
 * unabhaengig ein Item ueber {@code maxProgress} Ticks und erzeugt dabei kontinuierlich
 * {@code production} HE/Tick - 1:1 aus dem Original.
 * <p>
 * SCOPE-Entscheidung: Die Original-Fairness-Pruefung in {@code isItemValidForSlot} (verhindert
 * eine Warteschlange staerker zu befuellen als andere) entfaellt - jeder der 12 Eingabeslots
 * akzeptiert unabhaengig jeden gueltigen Brennstoff (kleinere QoL-Vereinfachung ohne
 * Gameplay-Auswirkung auf die Kernmechanik).
 */
public class MachineRadGenBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    public static final int QUEUE_COUNT = 12;
    public static final int SLOT_INPUT_START  = 0;
    public static final int SLOT_OUTPUT_START = 12;
    public static final int INVENTORY_SIZE    = 24;

    private static final long MAX_POWER = 1_000_000L;
    private static final long ENERGY_EXTRACT_RATE = 50_000L;

    private final int[] progress = new int[QUEUE_COUNT];
    private final int[] maxProgress = new int[QUEUE_COUNT];
    private final int[] production = new int[QUEUE_COUNT];
    private final ItemStack[] processing = new ItemStack[QUEUE_COUNT];

    private int output;
    private boolean isOn;

    public MachineRadGenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADGEN_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, ENERGY_EXTRACT_RATE);
    }

    @Override
    public int getCurrentMode() {
        return 2; // OUTPUT only, so the energy network treats this as a generator.
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRadGenBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick();
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();
        output = 0;

        for (int i = 0; i < QUEUE_COUNT; i++) {
            ItemStack input = inventory.getStackInSlot(SLOT_INPUT_START + i);
            if (processing[i] == null && !input.isEmpty()) {
                RadGenRecipe recipe = findRecipe(input);
                if (recipe != null && recipe.getDuration() > 0 && canAcceptOutput(i, recipe)) {
                    progress[i] = 0;
                    maxProgress[i] = recipe.getDuration();
                    production[i] = recipe.getPower();
                    processing[i] = new ItemStack(input.getItem(), 1);
                    input.shrink(1);
                    if (input.isEmpty()) inventory.setStackInSlot(SLOT_INPUT_START + i, ItemStack.EMPTY);
                    setChanged();
                }
            }
        }

        isOn = false;
        for (int i = 0; i < QUEUE_COUNT; i++) {
            if (processing[i] == null) continue;

            isOn = true;
            long space = getMaxEnergyStored() - getEnergyStored();
            long added = Math.min(production[i], space);
            setEnergyStored(getEnergyStored() + added);
            output += production[i];
            progress[i]++;

            if (progress[i] >= maxProgress[i]) {
                progress[i] = 0;
                RadGenRecipe recipe = findRecipe(processing[i]);
                if (recipe != null) {
                    ItemStack out = recipe.getOutput();
                    if (!out.isEmpty()) {
                        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT_START + i);
                        if (current.isEmpty()) {
                            inventory.setStackInSlot(SLOT_OUTPUT_START + i, out);
                        } else {
                            current.grow(out.getCount());
                        }
                    }
                }
                processing[i] = null;
                setChanged();
            }
        }

        sendUpdateToClient();
    }

    /** Data-driven поиск RadGenRecipe по входному стаку (заменяет статический RadGenRecipes.get). */
    @org.jetbrains.annotations.Nullable
    private RadGenRecipe findRecipe(ItemStack stack) {
        Level level = getLevel();
        if (level == null) return null;
        for (RadGenRecipe recipe : RecipeHooks.getAllRecipes(level, RadGenRecipe.Type.INSTANCE)) {
            if (recipe.matches(stack)) return recipe;
        }
        return null;
    }

    private boolean canAcceptOutput(int queue, RadGenRecipe recipe) {
        ItemStack result = recipe.getOutput();
        if (result.isEmpty()) return true;
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT_START + queue);
        if (current.isEmpty()) return true;
        return com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int getProgress(int queue)    { return progress[queue]; }
    public int getMaxProgress(int queue) { return maxProgress[queue]; }
    public boolean isProcessing(int queue) { return processing[queue] != null; }
    public int getOutput()   { return output; }
    public boolean isOn()    { return isOn; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putIntArray("progress", progress);
        tag.putIntArray("max_progress", maxProgress);
        tag.putIntArray("production", production);
        tag.putBoolean("is_on", isOn);

        ListTag list = new ListTag();
        for (int i = 0; i < QUEUE_COUNT; i++) {
            if (processing[i] != null) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("slot", (byte) i);
                entry.put("stack", com.hbm_m.platform.PlatformHooks.safeItemSave(processing[i], null));
                list.add(entry);
            }
        }
        tag.put("processing", list);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putIntArray("progress", progress);
        tag.putIntArray("max_progress", maxProgress);
        tag.putIntArray("production", production);
        tag.putBoolean("is_on", isOn);

        ListTag list = new ListTag();
        for (int i = 0; i < QUEUE_COUNT; i++) {
            if (processing[i] != null) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("slot", (byte) i);
                entry.put("stack", com.hbm_m.platform.PlatformHooks.safeItemSave(processing[i], registries));
                list.add(entry);
            }
        }
        tag.put("processing", list);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        int[] p = tag.getIntArray("progress");
        int[] mp = tag.getIntArray("max_progress");
        int[] pr = tag.getIntArray("production");
        for (int i = 0; i < QUEUE_COUNT; i++) {
            progress[i] = i < p.length ? p[i] : 0;
            maxProgress[i] = i < mp.length ? mp[i] : 0;
            production[i] = i < pr.length ? pr[i] : 0;
            processing[i] = null;
        }
        isOn = tag.getBoolean("is_on");

        ListTag list = tag.getList("processing", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("slot");
            if (slot >= 0 && slot < QUEUE_COUNT) {
                processing[slot] = com.hbm_m.platform.PlatformHooks.itemStackOf(entry.getCompound("stack"), registries);
            }
        }
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < SLOT_OUTPUT_START) {
            return findRecipe(stack) != null;
        }
        return false;
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.radgen");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineRadGenMenu.create(id, inventory, this);
    }
}
