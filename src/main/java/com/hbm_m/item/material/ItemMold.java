package com.hbm_m.item.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code ItemMold}: die Gussformen mit fester ID, Groesse (0 = flaches Becken, 1 = Giessbecken) und ihrem
 * Ergebnis je Material. Im Original ein Metadaten-Item, im Port je Form ein {@code mold_<name>}-Item
 * ({@link ItemCastMold}); {@link #getMold(ItemStack)} verbindet beides.
 */
public final class ItemMold {

    public static final List<Mold> molds = new ArrayList<>(); //molds in "pretty" order
    public static final Map<Integer, Mold> moldById = new HashMap<>();
    public static final Map<String, Mold> moldByName = new LinkedHashMap<>();

    public static final Map<NTMMaterial, Item> blockOverrides = new HashMap<>();

    private static boolean init = false;
    private static int nextOrder = 0;

    private ItemMold() { }

    private static Item port(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
        return item == Items.AIR ? null : item;
    }

    public static synchronized void init() {
        if (init) return;
        init = true;

        blockOverrides.put(Mats.MAT_STONE, Items.STONE);
        blockOverrides.put(Mats.MAT_OBSIDIAN, Items.OBSIDIAN);

        int S = 0;
        int L = 1;
        registerMold(new MoldShape(0, S, "nugget", MaterialShapes.NUGGET));
        registerMold(new MoldShape(1, S, "billet", MaterialShapes.BILLET));
        registerMold(new MoldShape(2, S, "ingot", MaterialShapes.INGOT));
        registerMold(new MoldShape(3, S, "plate", MaterialShapes.PLATE));
        registerMold(new MoldShape(4, S, "wire", MaterialShapes.WIRE, 8));

        registerMold(new MoldShape(19, S, "plate_cast", MaterialShapes.CASTPLATE));
        registerMold(new MoldShape(20, S, "wire_dense", MaterialShapes.DENSEWIRE));

        registerMold(new MoldMulti(5, S, "blade", MaterialShapes.INGOT.q(3),
                Mats.MAT_TITANIUM, port("blade_titanium"), 1,
                Mats.MAT_TUNGSTEN, port("blade_tungsten"), 1));

        registerMold(new MoldMulti(6, S, "blades", MaterialShapes.INGOT.q(4),
                Mats.MAT_STEEL, port("blades_steel"), 1,
                Mats.MAT_TITANIUM, port("blades_titanium"), 1));

        registerMold(new MoldMulti(7, S, "stamp", MaterialShapes.INGOT.q(4),
                Mats.MAT_STONE, port("stamp_stone_flat"), 1,
                Mats.MAT_IRON, port("stamp_iron_flat"), 1,
                Mats.MAT_STEEL, port("stamp_steel_flat"), 1,
                Mats.MAT_TITANIUM, port("stamp_titanium_flat"), 1,
                Mats.MAT_OBSIDIAN, port("stamp_obsidian_flat"), 1));

        registerMold(new MoldShape(8, S, "shell", MaterialShapes.SHELL));
        registerMold(new MoldShape(9, S, "pipe", MaterialShapes.PIPE));

        registerMold(new MoldShape(10, L, "ingots", MaterialShapes.INGOT, 9));
        registerMold(new MoldShape(11, L, "plates", MaterialShapes.PLATE, 9));
        registerMold(new MoldShape(13, L, "plates_cast", MaterialShapes.CASTPLATE, 3));
        registerMold(new MoldShape(21, L, "wires_dense", MaterialShapes.DENSEWIRE, 9));
        registerMold(new MoldBlock(12, L, "block", MaterialShapes.BLOCK));

        registerMold(new MoldMulti(16, S, "c9", MaterialShapes.PLATE.q(1, 4),
                Mats.MAT_GUNMETAL, port("casing_small"), 1,
                Mats.MAT_WEAPONSTEEL, port("casing_small_steel"), 1));
        registerMold(new MoldMulti(17, S, "c50", MaterialShapes.PLATE.q(1, 2),
                Mats.MAT_GUNMETAL, port("casing_large"), 1,
                Mats.MAT_WEAPONSTEEL, port("casing_large_steel"), 1));

        registerMold(new MoldShape(22, S, "barrel_light", MaterialShapes.LIGHTBARREL));
        registerMold(new MoldShape(23, S, "barrel_heavy", MaterialShapes.HEAVYBARREL));
        registerMold(new MoldShape(24, S, "receiver_light", MaterialShapes.LIGHTRECEIVER));
        registerMold(new MoldShape(25, S, "receiver_heavy", MaterialShapes.HEAVYRECEIVER));
        registerMold(new MoldShape(26, S, "mechanism", MaterialShapes.MECHANISM));
        registerMold(new MoldShape(27, S, "stock", MaterialShapes.STOCK));
        registerMold(new MoldShape(28, S, "grip", MaterialShapes.GRIP));
    }

    private static void registerMold(Mold mold) {
        molds.add(mold);
        moldById.put(mold.id, mold);
        moldByName.put(mold.name, mold);
    }

    /** Die Form eines Port-Formen-Items ({@code mold_<name>}), oder {@code null} fuer Formen ohne Gegenstueck im Original. */
    @Nullable
    public static Mold getMold(ItemStack stack) {
        init();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemCastMold)) return null;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!path.startsWith("mold_")) return null;
        return moldByName.get(path.substring("mold_".length()));
    }

    /** Das Port-Item einer Form. */
    @Nullable
    public static Item itemOf(Mold mold) {
        return port("mold_" + mold.name);
    }

    public static Component sizeName(Mold mold) {
        return mold.size == 0 ? Component.translatable("block.hbm_m.foundry_mold") : Component.translatable("block.hbm_m.foundry_basin");
    }

    public abstract static class Mold {
        public final int order;
        public final int id;
        public final int size;
        public final String name;

        public Mold(int id, int size, String name) {
            this.order = nextOrder++;
            this.id = id;
            this.size = size;
            this.name = name;
        }

        @Nullable
        public abstract ItemStack getOutput(NTMMaterial mat);
        public abstract int getCost();
        public abstract Component getTitle();
    }

    public static class MoldShape extends Mold {

        public final MaterialShapes shape;
        public final int amount;

        public MoldShape(int id, int size, String name, MaterialShapes shape) {
            this(id, size, name, shape, 1);
        }

        public MoldShape(int id, int size, String name, MaterialShapes shape, int amount) {
            super(id, size, name);
            this.shape = shape;
            this.amount = amount;
        }

        @Override
        @Nullable
        public ItemStack getOutput(NTMMaterial mat) {
            Item item = Mats.getItemForShape(mat, shape);
            if (item == null) return null;
            return new ItemStack(item, this.amount);
        }

        @Override
        public int getCost() {
            return shape.q(amount);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("shape." + shape.name()).append(" x" + amount);
        }
    }

    public static class MoldBlock extends MoldShape {

        public MoldBlock(int id, int size, String name, MaterialShapes shape) {
            super(id, size, name, shape);
        }

        @Override
        @Nullable
        public ItemStack getOutput(NTMMaterial mat) {
            Item override = blockOverrides.get(mat);
            if (override != null) return new ItemStack(override);
            return super.getOutput(mat);
        }
    }

    /* not so graceful but it does the job and it does it well */
    public static class MoldMulti extends Mold {

        public final Map<NTMMaterial, ItemStack> map = new HashMap<>();
        public final int amount;
        public int stacksize;

        /** Eintraege: Material, Item (darf fehlen), Stapelgroesse. */
        public MoldMulti(int id, int size, String name, int amount, Object... inputs) {
            super(id, size, name);
            this.amount = amount;

            for (int i = 0; i < inputs.length; i += 3) {
                Item item = (Item) inputs[i + 1];
                int count = (int) inputs[i + 2];
                if (i == 0) stacksize = count;
                if (item != null) map.put((NTMMaterial) inputs[i], new ItemStack(item, count));
            }
        }

        @Override
        @Nullable
        public ItemStack getOutput(NTMMaterial mat) {
            ItemStack out = this.map.get(mat);
            return out != null ? out.copy() : null;
        }

        @Override
        public int getCost() {
            return amount;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("shape." + name).append(" x" + this.stacksize);
        }
    }
}
