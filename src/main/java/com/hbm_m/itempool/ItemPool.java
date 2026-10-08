package com.hbm_m.itempool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.itempool.ItemPool}: benannte, gewichtete Beutelisten ({@code WeightedRandomChestContent[]}),
 * die Strukturen, Tresore und Lootbloecke befuellen.
 *
 * <p>Abweichung durch 1.20: Eintraege werden als Registernamen bzw. Stack-Lieferanten abgelegt und erst beim
 * ersten Zugriff aufgeloest (die Pools werden vor dem Abschluss der Registrierung gebaut). Gegenstaende ohne
 * Port-Gegenstueck fallen beim Aufloesen heraus.</p>
 */
public class ItemPool {

    private static boolean initialized = false;

    /** Original {@code ItemPool.initialize()} - hier faul beim ersten Zugriff. */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ItemPoolsLegacy.init();
        ItemPoolsComponent.init();
        ItemPoolsSingle.init();
        ItemPoolsPile.init();
        // TODO(port): ItemPoolsRedRoom/Satellite/C130/VendingMachine leben im Port bei ihren Verwendern.
    }

    public static final Map<String, ItemPool> pools = new HashMap<>();

    public String name;
    public WeightedContent[] pool = new WeightedContent[0];

    private final List<Entry> buildingList = new ArrayList<>();

    public ItemPool() { }

    public ItemPool(String name) {
        this.name = name;
        pools.put(name, this);
    }

    /** Eintrag ueber den Registernamen ({@code "minecraft:paper"} oder ohne Namensraum fuer {@code hbm_m}). */
    public ItemPool add(String id, int min, int max, int weight) {
        return add(id, 0, min, max, weight);
    }

    /** Wie oben, {@code damage} entspricht der Meta eines beschaedigbaren Gegenstands (z.B. Gasmaske 60). */
    public ItemPool add(String id, int damage, int min, int max, int weight) {
        buildingList.add(new Entry(() -> {
            ResourceLocation rl = id.contains(":") ? new ResourceLocation(id) : new ResourceLocation(RefStrings.MODID, id);
            Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item == null || item == Items.AIR) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(item);
            if (damage > 0 && stack.isDamageableItem()) stack.setDamageValue(damage);
            return stack;
        }, min, max, weight, id));
        return this;
    }

    public ItemPool add(Supplier<ItemStack> stack, int min, int max, int weight) {
        buildingList.add(new Entry(stack, min, max, weight, "special"));
        return this;
    }

    public ItemPool build() {
        // aufgeloest wird beim ersten getPool
        return this;
    }

    private boolean resolved = false;

    private synchronized WeightedContent[] resolve() {
        if (resolved) return pool;
        resolved = true;
        List<WeightedContent> list = new ArrayList<>();
        for (Entry e : buildingList) {
            ItemStack stack = ItemStack.EMPTY;
            try { stack = e.stack.get(); } catch (Exception ex) { /* nicht portiert */ }
            if (stack.isEmpty()) {
                MainRegistry.LOGGER.debug("[ItemPool] {}: Eintrag {} hat kein Port-Gegenstueck", name, e.debugName);
                continue;
            }
            list.add(new WeightedContent(stack, e.min, e.max, e.weight));
        }
        pool = list.toArray(new WeightedContent[0]);
        buildingList.clear();
        return pool;
    }

    /** Liefert den Pool, ist er nicht vorhanden, den Ersatzpool. */
    public static WeightedContent[] getPool(String name) {
        initialize();
        ItemPool pool = pools.get(name);
        if (pool == null) return backupPool();
        WeightedContent[] p = pool.resolve();
        return p.length == 0 ? backupPool() : p;
    }

    public static ItemStack getStack(String pool, RandomSource rand) {
        return getStack(getPool(pool), rand);
    }

    public static ItemStack getStack(WeightedContent[] pool, RandomSource rand) {
        WeightedContent weighted = getRandomItem(rand, pool);
        ItemStack stack = weighted.stack.copy();
        stack.setCount(weighted.min + rand.nextInt(weighted.max - weighted.min + 1));
        return stack;
    }

    /** Vanilla 1.7.10 {@code WeightedRandom.getRandomItem}. */
    public static WeightedContent getRandomItem(RandomSource rand, WeightedContent[] pool) {
        int total = 0;
        for (WeightedContent c : pool) total += c.weight;
        if (total <= 0) throw new IllegalArgumentException();
        int r = rand.nextInt(total);
        for (WeightedContent c : pool) {
            r -= c.weight;
            if (r < 0) return c;
        }
        return null;
    }

    /**
     * 1:1 {@code WeightedRandomChestContent.generateChestContents}: {@code count} Ziehungen, jede in einen
     * zufaelligen Slot (ueberschreibt ggf.). Uebersteigt die Menge die Stapelgroesse, wird in Einzelstuecke zerlegt.
     */
    public static void generateChestContents(RandomSource rand, WeightedContent[] pool, Object inv, int count) {
        Inv view = view(inv);
        if (view == null || view.size() <= 0) return;
        for (int j = 0; j < count; ++j) {
            WeightedContent c = getRandomItem(rand, pool);
            for (ItemStack item : c.generate(rand)) {
                view.set(rand.nextInt(view.size()), item);
            }
        }
        if (inv instanceof net.minecraft.world.level.block.entity.BlockEntity be) be.setChanged();
    }

    public static void generateChestContents(Random rand, WeightedContent[] pool, Object inv, int count) {
        if (inv == null) return;
        generateChestContents(RandomSource.create(rand.nextLong()), pool, inv, count);
    }

    /** Port: Behaelter sind teils {@link Container}, teils Forge-ItemStackHandler (Kisten, Aktenschrank). */
    public interface Inv {
        int size();
        void set(int slot, ItemStack stack);
    }

    public static boolean isInventory(Object te) {
        return view(te) != null;
    }

    public static Inv view(Object te) {
        if (te == null) return null;
        if (te instanceof Container c) {
            return new Inv() {
                public int size() { return c.getContainerSize(); }
                public void set(int slot, ItemStack stack) { c.setItem(slot, stack); }
            };
        }
        if (te instanceof com.hbm_m.blockentity.crates.BaseCrateBlockEntity crate) {
            return handler(crate.getItemHandler());
        }
        if (te instanceof com.hbm_m.blockentity.decorations.FileCabinetBlockEntity cabinet) {
            return handler(cabinet.items);
        }
        //? if forge {
        if (te instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            var cap = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).resolve();
            if (cap.isPresent() && cap.get() instanceof net.minecraftforge.items.IItemHandlerModifiable h) return handler(h);
        }
        //?}
        return null;
    }

    //? if forge {
    private static Inv handler(net.minecraftforge.items.IItemHandlerModifiable h) {
        return new Inv() {
            public int size() { return h.getSlots(); }
            public void set(int slot, ItemStack stack) { h.setStackInSlot(slot, stack); }
        };
    }
    //?}

    /** Ersatzpool des Originals. */
    private static WeightedContent[] backupPool;

    private static WeightedContent[] backupPool() {
        if (backupPool == null) {
            List<WeightedContent> l = new ArrayList<>();
            l.add(new WeightedContent(new ItemStack(Items.BREAD), 1, 3, 10));
            l.add(new WeightedContent(new ItemStack(Items.STICK), 2, 5, 10));
            BuiltInRegistries.ITEM.getOptional(new ResourceLocation(RefStrings.MODID, "scrap")).ifPresent(i -> l.add(new WeightedContent(new ItemStack(i), 1, 3, 10)));
            BuiltInRegistries.ITEM.getOptional(new ResourceLocation(RefStrings.MODID, "dust")).ifPresent(i -> l.add(new WeightedContent(new ItemStack(i), 2, 5, 5)));
            backupPool = l.toArray(new WeightedContent[0]);
        }
        return backupPool;
    }

    private record Entry(Supplier<ItemStack> stack, int min, int max, int weight, String debugName) { }

    /** 1:1 {@code WeightedRandomChestContent}. */
    public static class WeightedContent {

        public final ItemStack stack;
        public final int min;
        public final int max;
        public final int weight;

        public WeightedContent(ItemStack stack, int min, int max, int weight) {
            this.stack = stack;
            this.min = min;
            this.max = max;
            this.weight = weight;
        }

        /** Forge 1.7.10 {@code ChestGenHooks.generateStacks}. */
        public ItemStack[] generate(RandomSource rand) {
            int count = min + rand.nextInt(max - min + 1);
            if (count <= 0) return new ItemStack[] { ItemStack.EMPTY };
            ItemStack[] ret;
            if (count <= stack.getMaxStackSize()) {
                ItemStack s = stack.copy();
                s.setCount(count);
                ret = new ItemStack[] { s };
            } else {
                ret = new ItemStack[count];
                for (int x = 0; x < count; x++) {
                    ret[x] = stack.copy();
                    ret[x].setCount(1);
                }
            }
            return ret;
        }
    }
}
