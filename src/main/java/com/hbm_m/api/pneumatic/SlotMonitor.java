package com.hbm_m.api.pneumatic;

import java.util.Iterator;
import java.util.LinkedHashSet;

import com.hbm_m.api.pneumatic.StackCache.CacheSlot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code SlotMonitor} (1.7.10): der Waechter ueber genau einen Lagerplatz.
 *
 * <p>Jedes Lager stellt fuer jeden seiner Plaetze einen solchen Waechter. Seine einzige Aufgabe ist
 * es, Veraenderungen zu bemerken und weiterzumelden - liegt jetzt etwas anderes darin, ist die
 * Menge gestiegen oder gefallen, oder ist der Platz fuer ein Terminal ueberhaupt nicht mehr
 * erreichbar.</p>
 *
 * <p>Diese Meldekette ist der Grund, warum das Lagernetz auch mit sehr vielen Plaetzen laeuft: die
 * Terminals rechnen nie selbst nach, sie bekommen die Aenderung zugestellt.</p>
 */
public class SlotMonitor {

    /** Der Platz, den dieser Waechter beobachtet. */
    public final int index;
    public final ISlotMonitorProvider parent;

    /** Alle zusammengefassten Plaetze, die diesen Waechter mitzaehlen. */
    public final LinkedHashSet<CacheSlot> viewedBy = new LinkedHashSet<>();

    @Nullable public Item item;
    public long stacksize;
    @Nullable public CompoundTag nbt;

    protected boolean hasAvailabilityChanged;
    protected boolean forceTypeUpdate;

    public SlotMonitor(int index, ISlotMonitorProvider parent) {
        this.index = index;
        this.parent = parent;
        this.hasAvailabilityChanged = true;
        this.forceTypeUpdate = true;
    }

    /** Ein Abbild des Inhalts ohne Menge - daraus baut der Zwischenspeicher seinen Anzeigestapel. */
    @Nullable
    public ItemStack toZeroStack() {
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, 0);
        if (nbt != null) stack.setTag(nbt.copy());
        return stack;
    }

    /**
     * Original: {@code availabilityHasChanged}. Das Lager meldet selbst, wenn sich die
     * Erreichbarkeit geaendert hat - etwa weil die Druckluft ausging oder die Druckstufe
     * umgestellt wurde. So muss die Reichweite nicht jeden Tick nachgerechnet werden.
     */
    public void availabilityHasChanged() {
        this.hasAvailabilityChanged = true;
    }

    /** 1:1-Port von {@code checkUpdate}: die eigentliche Pruefung, einmal je Tick. */
    public void checkUpdate() {

        PneumaticNet net = parent.getRelevantNetwork();

        if (hasAvailabilityChanged) {
            if (net != null) {
                for (StackCache cache : net.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) cache.addToCache(this);
                }
            }

            // Wer diesen Platz nicht mehr erreicht, verliert ihn wieder.
            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                CacheSlot slot = iterator.next();
                StackCache cache = slot.getStackCache();
                if (cache.hasExpired || !parent.isAvailableToCache(cache)) {
                    slot.removeMonitor(this);
                    iterator.remove();
                }
            }

            hasAvailabilityChanged = false;
        }

        ItemStack stack = parent.getSlotAt(index);
        long amount = parent.getAmountAt(index);

        boolean empty = stack == null || stack.isEmpty();
        boolean hasTypeChanged;

        if (empty || item == null) {
            hasTypeChanged = empty != (item == null);
        } else if (item != stack.getItem()) {
            hasTypeChanged = true;
        } else {
            CompoundTag tag = stack.getTag();
            hasTypeChanged = (nbt == null) != (tag == null) || (nbt != null && !nbt.equals(tag));
        }

        if (hasTypeChanged || forceTypeUpdate) {

            // Aus allen bisherigen Sammelplaetzen austragen ...
            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                iterator.next().removeMonitor(this);
                iterator.remove();
            }

            // ... den neuen Inhalt uebernehmen ...
            if (empty) {
                item = null;
                stacksize = 0;
                nbt = null;
            } else {
                item = stack.getItem();
                stacksize = amount;
                nbt = stack.getTag() != null ? stack.getTag().copy() : null;
            }

            // ... und sich neu eintragen.
            if (net != null) {
                for (StackCache cache : net.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) cache.addToCache(this);
                }
            }

            forceTypeUpdate = false;
            return;
        }

        if (stacksize != amount) {
            long delta = amount - stacksize;
            for (CacheSlot slot : viewedBy) slot.changeAmounts(delta);
            stacksize = amount;
        }
    }
}
