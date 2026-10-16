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
 * SCOPE-Entscheidung: Aktuell speist noch keine andere Maschine dieses Ports in den Ash Pit ein
 * (Firebox/WoodBurner/Chimney-Feuerungslogik ist noch nicht so weit integriert) - {@code addAsh}
 * steht bereit, sobald diese Maschinen portiert/erweitert werden, analog zur bereits vorhandenen
 * {@code IHeatSource}-Schnittstelle.
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

    public MachineAshpitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASHPIT_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAshpitBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick();
        }
    }

    private void serverTick() {
        boolean dirty = false;
        for (AshType type : AshType.values()) {
            if (processAsh(type)) dirty = true;
        }
        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
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
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (AshType type : AshType.values()) {
            ashLevel[type.ordinal()] = tag.getInt("ash_" + type.name());
        }
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
}
