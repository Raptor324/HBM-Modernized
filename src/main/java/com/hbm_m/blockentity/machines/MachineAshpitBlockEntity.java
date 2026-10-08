package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ModItems;

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
 * Ash Pit - Port von {@code TileEntityAshpit} (1.7.10 Original). Sammelt Asche aus 5 Kategorien
 * (Holz/Kohle/Sonstiges/Flug-/Feinasche), die im Original von darueberstehenden Feuerungen
 * (Firebox, WoodBurner, FurnaceBrick, Chimney) per direktem Feldzugriff ({@code ashLevelWood += ..})
 * eingespeist wird - hier ueber {@link #addAsh(AshType, int)} oeffentlich zugaenglich gemacht.
 * Sobald ein Schwellwert erreicht ist, wird ein Aschepulver-Item in einen freien/passenden der 5
 * Ausgabeslots gelegt (1:1 aus {@code processAsh}).
 * <p>
 * <p>Gefuettert wird sie von der {@link MachineFireboxBlockEntity Feuerbuechse} direkt darueber -
 * je nach Brennstoff faellt Holz-, Kohle- oder sonstige Asche an. Weitere Feuerungen des Originals
 * (Holzbrenner, Ziegelofen, Schornstein) koennen sich ueber {@link #addAsh(AshType, int)}
 * anhaengen, sobald sie so weit sind.
 */
public class MachineAshpitBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 5;

    public enum AshType {
        WOOD(2000, ModItems.ASH_WOOD),
        COAL(2000, ModItems.ASH_COAL),
        MISC(2000, ModItems.ASH_MISC),
        FLY(2000, ModItems.ASH_FLY),
        SOOT(8000, ModItems.ASH_SOOT);

        final int threshold;
        final dev.architectury.registry.registries.RegistrySupplier<Item> item;

        AshType(int threshold, dev.architectury.registry.registries.RegistrySupplier<Item> item) {
            this.threshold = threshold;
            this.item = item;
        }
    }

    private final int[] ashLevel = new int[AshType.values().length];

    /** Original {@code playersUsing}/{@code isFull}: synchronisiert fuer die Klappe und die Glut (RenderAshpit). */
    private int playersUsing = 0;
    public boolean isFull;
    /** Original {@code doorAngle/prevDoorAngle} - nur Client. */
    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    public MachineAshpitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASHPIT_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAshpitBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick();
        } else {
            // Original isRemote-Zweig: Klappe schwingt mit (doorAngle / 10) + 3 je Tick auf 0..135
            be.prevDoorAngle = be.doorAngle;
            float swingSpeed = (be.doorAngle / 10F) + 3;
            if (be.playersUsing > 0) {
                be.doorAngle += swingSpeed;
            } else {
                be.doorAngle -= swingSpeed;
            }
            be.doorAngle = net.minecraft.util.Mth.clamp(be.doorAngle, 0F, 135F);
        }
    }

    private void serverTick() {
        boolean dirty = false;
        for (AshType type : AshType.values()) {
            if (processAsh(type)) dirty = true;
        }
        // Original openInventory/closeInventory: hier ueber die offenen Menues gezaehlt.
        int using = 0;
        for (Player p : level.players()) {
            if (p.containerMenu instanceof com.hbm_m.inventory.menu.MachineAshpitMenu m && m.getBlockEntity() == this) using++;
        }
        boolean full = false;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) full = true;
        }
        boolean sync = using != playersUsing || full != isFull;
        playersUsing = using;
        isFull = full;
        if (dirty) setChanged();
        if (dirty || sync) sendUpdateToClient();
    }

    /** Von Feuerungs-Maschinen aufzurufen, die unter sich einen Ash Pit finden (siehe Klassenkommentar). */
    public void addAsh(AshType type, int amount) {
        ashLevel[type.ordinal()] += amount;
    }

    private boolean processAsh(AshType type) {
        if (ashLevel[type.ordinal()] < type.threshold) return false;

        ItemStack toAdd = new ItemStack(type.item.get(), 1);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (current.isEmpty()) {
                inventory.setStackInSlot(i, toAdd);
                ashLevel[type.ordinal()] -= type.threshold;
                return true;
            } else if (current.getCount() < current.getMaxStackSize()
                    && com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, toAdd)) {
                current.grow(1);
                ashLevel[type.ordinal()] -= type.threshold;
                return true;
            }
        }
        return false;
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (AshType type : AshType.values()) {
            tag.putInt("ash_" + type.name(), ashLevel[type.ordinal()]);
        }
        tag.putInt("playersUsing", playersUsing);
        tag.putBoolean("isFull", isFull);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (AshType type : AshType.values()) {
            ashLevel[type.ordinal()] = tag.getInt("ash_" + type.name());
        }
        playersUsing = tag.getInt("playersUsing");
        isFull = tag.getBoolean("isFull");
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Nur Ausgabe - siehe Klassenkommentar.
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.ashpit");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineAshpitMenu.create(id, inventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0-4}; nichts hinein, alles heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1, 2, 3, 4 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
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
    //?}
}
