package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.RenderBounds;

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
 * Strom geht hinten ({@code -4 dir}) heraus; die Fairness-Pruefung beim Befuellen verteilt Brennstoff gleichmaessig
 * auf die Warteschlangen.
 */
public class MachineRadGenBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    public static final int QUEUE_COUNT = 12;
    public static final int SLOT_INPUT_START  = 0;
    public static final int SLOT_OUTPUT_START = 12;
    public static final int INVENTORY_SIZE    = 24;

    private static final long MAX_POWER = 1_000_000L;
    private static final long ENERGY_EXTRACT_RATE = MAX_POWER;

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
        output = 0;

        // Original: tryProvide(x - dir*4, y, z - dir*4, dir.getOpposite())
        if (level instanceof net.minecraft.server.level.ServerLevel world) {
            net.minecraft.core.Direction dir = getBlockState().getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING);
            BlockPos p = worldPosition.relative(dir, -4);
            this.tryProvide(world, p.getX(), p.getY(), p.getZ(), dir.getOpposite());
        }

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
    public int getProduction(int queue) { return production[queue]; }

    /** Original: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }
    public boolean isProcessing(int queue) { return processing[queue] != null; }
    public int getOutput()   { return output; }
    public boolean isOn()    { return isOn; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
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

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
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
        // Original: nur gueltiger Brennstoff, und kein Slot darf voller werden als ein anderer mit gleichem Inhalt
        if (slot >= SLOT_OUTPUT_START) return false;
        RadGenRecipe recipe = findRecipe(stack);
        if (recipe == null || recipe.getDuration() <= 0) return false;

        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) return true;

        int size = current.getCount();

        for (int j = 0; j < QUEUE_COUNT; j++) {
            ItemStack other = inventory.getStackInSlot(j);
            if (other.isEmpty()) return false;
            if (other.getItem() == stack.getItem() && other.getCount() < size) return false;
        }

        return true;
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

    //? if forge {
    /** Original {@code ISidedInventory}: alle 24 Plaetze; Brennstoff gleichmaessig in 0-11, Reste aus 12-23 heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, 23); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (slot >= 12 || !isItemValidForSlot(slot, stack)) return false;
                    if (inventory.getStackInSlot(slot).isEmpty()) return true;
                    int size = inventory.getStackInSlot(slot).getCount();
                    for (int j = 0; j < 12; j++) {
                        net.minecraft.world.item.ItemStack s = inventory.getStackInSlot(j);
                        if (s.isEmpty()) return false;
                        if (net.minecraft.world.item.ItemStack.isSameItem(s, stack) && s.getCount() < size) return false;
                    }
                    return true; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 12; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: alle 24 Plaetze; Brennstoff gleichmaessig in 0-11, Reste aus 12-23 heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, 23); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (slot >= 12 || !isItemValidForSlot(slot, stack)) return false;
                    if (inventory.getStackInSlot(slot).isEmpty()) return true;
                    int size = inventory.getStackInSlot(slot).getCount();
                    for (int j = 0; j < 12; j++) {
                        net.minecraft.world.item.ItemStack s = inventory.getStackInSlot(j);
                        if (s.isEmpty()) return false;
                        if (net.minecraft.world.item.ItemStack.isSameItem(s, stack) && s.getCount() < size) return false;
                    }
                    return true; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 12; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
