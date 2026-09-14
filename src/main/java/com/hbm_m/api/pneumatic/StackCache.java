package com.hbm_m.api.pneumatic;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code StackCache} (1.7.10): das Verzeichnis eines Zugangspunktes.
 *
 * <p>Jeder Zugangspunkt - ein Terminal, ein Ein- oder Ausgabegeraet - haelt genau einen
 * Zwischenspeicher. Darin steht, welche Gegenstaende er im Netz ueberhaupt erreichen kann und
 * wieviel davon. Gleichartige Plaetze aus verschiedenen Lagern werden dabei zu <b>einem</b>
 * {@link CacheSlot} zusammengefasst, dessen Stapelgroesse nicht bei 64 endet.</p>
 *
 * <p>Der Zwischenspeicher rechnet nichts selbst nach. Er wird von den {@link SlotMonitor
 * Platzwaechtern} der Lager benachrichtigt, sobald sich dort etwas aendert - das haelt den Aufwand
 * auch bei tausenden Plaetzen klein.</p>
 */
public class StackCache {

    public final BlockPos pos;
    public boolean hasExpired = false;

    /** Kennzahl eines Gegenstands zu dem zusammengefassten Platz. */
    public final Map<Long, CacheSlot> cacheSlots = new LinkedHashMap<>();

    public StackCache(BlockPos pos) {
        this.pos = pos.immutable();
    }

    public void addToCache(SlotMonitor monitor) {
        long identity = getStackIdentity(monitor.item, monitor.nbt);
        CacheSlot cache = cacheSlots.computeIfAbsent(identity, k -> new CacheSlot(monitor.toZeroStack()));
        cache.addMonitor(monitor);
    }

    @Nullable
    public CacheSlot getSlotFromStack(ItemStack stack) {
        return cacheSlots.get(getStackIdentity(stack));
    }

    /**
     * Original: {@code consumeItemsAndReturnQuantity} - so viele Stueck wie moeglich entnehmen und
     * zurueckgeben, wieviele es wirklich waren.
     */
    public long consumeItemsAndReturnQuantity(ItemStack stack, long amount) {
        long identity = getStackIdentity(stack);
        CacheSlot cache = cacheSlots.get(identity);
        if (cache == null) return 0L;

        long original = amount;

        for (SlotMonitor monitor : new LinkedHashSet<>(cache.monitors)) {
            ItemStack inSlot = monitor.parent.getSlotAt(monitor.index);
            if (getStackIdentity(inSlot) != identity) continue;

            amount = monitor.parent.useUpItem(monitor.index, amount);
            if (amount <= 0) break;
        }

        return original - amount;
    }

    /**
     * Original: {@code addItemsAndReturnQuantity} - erst in Plaetze desselben Gegenstands, dann in
     * leere Plaetze, die eine Typfestlegung zulassen. Zurueck kommt, was nicht mehr hineinpasste.
     */
    public long addItemsAndReturnQuantity(ItemStack stack, long amount) {
        long identity = getStackIdentity(stack);
        CacheSlot cache = cacheSlots.get(identity);

        if (cache != null) {
            for (SlotMonitor monitor : new LinkedHashSet<>(cache.monitors)) {
                ItemStack inSlot = monitor.parent.getSlotAt(monitor.index);
                if (getStackIdentity(inSlot) != identity) continue;

                amount = monitor.parent.addItem(monitor.index, amount);
                if (amount <= 0) break;
            }
        }

        if (amount > 0) {
            CacheSlot emptySlots = cacheSlots.get(getNullIdentity());
            if (emptySlots != null) {
                for (SlotMonitor monitor : new LinkedHashSet<>(emptySlots.monitors)) {
                    if (!monitor.parent.allowTypeSetting()) continue;
                    if (!monitor.parent.getSlotAt(monitor.index).isEmpty()) continue;

                    amount = monitor.parent.setupType(monitor.index, stack, amount);
                    if (amount <= 0) break;
                }
            }
        }

        return amount;
    }

    public void dissolveCache() {
        for (CacheSlot slot : cacheSlots.values()) slot.destroy();
        cacheSlots.clear();
        hasExpired = true;
    }

    /**
     * 1:1-Port von {@code CacheSlot}: mehrere gleichartige Plaetze zu einem zusammengefasst - ein
     * Platz mit unbegrenzter Stapelgroesse, der auf die einzelnen Waechter zeigt.
     */
    public class CacheSlot {

        @Nullable public final ItemStack displayStack;
        public long stacksize;

        public final LinkedHashSet<SlotMonitor> monitors = new LinkedHashSet<>();

        public CacheSlot(@Nullable ItemStack stack) {
            if (stack != null && !stack.isEmpty()) {
                this.displayStack = stack.copy();
                this.displayStack.setCount(1);
                this.stacksize = stack.getCount();
            } else {
                this.displayStack = null;
                this.stacksize = 0;
            }
        }

        public void addMonitor(SlotMonitor monitor) {
            if (monitors.add(monitor)) {
                monitor.viewedBy.add(this);
                changeAmounts(monitor.stacksize);
            }
        }

        public void removeMonitor(SlotMonitor monitor) {
            if (monitors.remove(monitor)) {
                changeAmounts(-monitor.stacksize);
            }
        }

        public void destroy() {
            for (SlotMonitor monitor : monitors) monitor.viewedBy.remove(this);
            stacksize = 0;
        }

        public void changeAmounts(long delta) {
            stacksize += delta;
        }

        public StackCache getStackCache() {
            return StackCache.this;
        }
    }

    // ── Kennzahlen ──────────────────────────────────────────────────────────

    public static long getNullIdentity() {
        return 0L;
    }

    /**
     * Original: {@code getStackIdentity}. In 1.20 gibt es keine Metadaten mehr, darum zaehlen nur
     * noch der Gegenstand und sein NBT.
     */
    public static long getStackIdentity(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return getNullIdentity();
        return getStackIdentity(stack.getItem(), stack.getTag());
    }

    public static long getStackIdentity(@Nullable Item item, @Nullable CompoundTag nbt) {
        if (item == null) return getNullIdentity();

        long identity = (long) BuiltInRegistries.ITEM.getId(item) * 27644437L;
        identity *= 27644437L;
        if (nbt != null) identity += nbt.hashCode();
        return identity;
    }
}
