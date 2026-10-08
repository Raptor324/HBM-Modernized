package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code PedestalRecipes}: Sockel-Rezepte fuer Legendary-Waffen und Geheimmunition. Neun Sockel (Mitte plus je
 * zwei Bloecke diagonal bzw. drei Bloecke gerade versetzt) werden per Redstone am mittleren Sockel ausgewertet
 * ({@link com.hbm_m.block.decorations.PedestalBlock}). Eingaben muessen in Gegenstand UND Anzahl exakt stimmen.
 * Das Original laedt/schreibt zusaetzlich {@code hbmPedestal.json}; hier ist die Liste wie die Standardwerte fest
 * im Code.
 *
 * <p>OreDict-Eingaben ({@code PB.plate()} usw.) sind im Port die entsprechenden Material-Gegenstaende.</p>
 */
public class PedestalRecipes {

    public static List<PedestalRecipe> recipes = new ArrayList<>();
    @SuppressWarnings("unchecked")
    public static ArrayList<PedestalRecipe>[] recipeSets = new ArrayList[2];

    static { for (int i = 0; i < recipeSets.length; i++) recipeSets[i] = new ArrayList<>(); }

    private static boolean initialized = false;

    /** Erst nach der Registrierung aufrufen (Waffen entstehen beim ersten Registry-Zugriff). */
    public static synchronized List<PedestalRecipe> getRecipes() {
        if (!initialized) {
            initialized = true;
            registerDefaults();
        }
        return recipes;
    }

    public static void registerDefaults() {

        register(new PedestalRecipe(gun("gun_light_revolver_dani"),
                null,                           mat(ModMaterials.LEAD, MaterialShape.PLATE),        null,
                mat(ModMaterials.GOLD, MaterialShape.PLATE), gunIn("gun_light_revolver"),            mat(ModMaterials.GOLD, MaterialShape.PLATE),
                null,                           mat(ModMaterials.LEAD, MaterialShape.PLATE),        null));

        register(new PedestalRecipe(gun("gun_maresleg_broken"),
                id("barbed_wire"),                                  mat(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE), id("barbed_wire"),
                mat(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE), gunIn("gun_maresleg"),                              mat(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE),
                id("barbed_wire"),                                  mat(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE), id("barbed_wire")));

        register(new PedestalRecipe(gun("gun_heavy_revolver_lilmac"),
                null,                   mod(EnumModSpecial.SCOPE),          null,
                id("magic_powder"),     gunIn("gun_heavy_revolver"),        mat(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE),
                null,                   id("part_grip_ivory"),              new PedestalInput(() -> Items.APPLE, 3)));

        register(new PedestalRecipe(gun("gun_heavy_revolver_protege"),
                id("dungeon_chain", 16),    id("cinnebar"),                 id("dungeon_chain", 16),
                id("scrap_nuclear"),        gunIn("gun_heavy_revolver"),    id("scrap_nuclear"),
                id("dungeon_chain", 16),    id("cinnebar"),                 id("dungeon_chain", 16)));

        register(new PedestalRecipe(gun("gun_amat_subtlety"),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT),       mat(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST),  mat(ModMaterials.STARMETAL, MaterialShape.INGOT),
                mat(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST),  gunIn("gun_amat"),                                      mat(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT),       mat(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST),  mat(ModMaterials.STARMETAL, MaterialShape.INGOT)));

        register(new PedestalRecipe(gun("gun_amat_penance"),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT),   mat(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST),    mat(ModMaterials.STARMETAL, MaterialShape.INGOT),
                mod(EnumModSpecial.SILENCER),                       gunIn("gun_amat"),                                          mod(EnumModSpecial.FURNITURE_BLACK),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT),   mat(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST),    mat(ModMaterials.STARMETAL, MaterialShape.INGOT)));

        register(new PedestalRecipe(gun("gun_flamer_daybreaker"),
                mat(ModMaterials.GOLD, MaterialShape.PLATE_CAST),   id("canned_slime"),     mat(ModMaterials.GOLD, MaterialShape.PLATE_CAST),
                mat(ModMaterials.PHOSPHORUS, MaterialShape.INGOT),  gunIn("gun_flamer"),    mat(ModMaterials.PHOSPHORUS, MaterialShape.INGOT),
                mat(ModMaterials.GOLD, MaterialShape.PLATE_CAST),   id("stick_dynamite"),   mat(ModMaterials.GOLD, MaterialShape.PLATE_CAST))
                .extra(PedestalExtraCondition.SUN));

        register(new PedestalRecipe(gun("gun_autoshotgun_sexy"),
                id("bolt_spike", 16),   id("wild_p"),                                           id("bolt_spike", 16),
                id("card_qos"),         gunIn("gun_autoshotgun"),                               id("card_aos"),
                id("bolt_spike", 16),   mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 16),   id("bolt_spike", 16)));

        register(new PedestalRecipe(gun("gun_minigun_lacunae"),
                null,                                   id("magic_powder", 4),  null,
                id("item_secret_selenium_steel", 4),    gunIn("gun_minigun"),   id("item_secret_selenium_steel", 4),
                null,                                   id("magic_powder", 4),  null)
                .extra(PedestalExtraCondition.FULL_MOON));

        register(new PedestalRecipe(gun("gun_laser_pistol_morning_glory"),
                null,                                   id("morning_glory", 1),             null,
                id("item_secret_selenium_steel", 2),    gunIn("gun_laser_pistol"),          id("item_secret_selenium_steel", 2),
                null,                                   new PedestalInput(() -> Items.EMERALD, 16), null));

        register(new PedestalRecipe(gun("gun_folly"),
                id("item_secret_folly", 4),                         id("item_secret_controller", 2),                        id("item_secret_folly", 4),
                mat(ModMaterials.BSCCO, MaterialShape.INGOT, 16),   mat(ModMaterials.STARMETAL, MaterialShape.BLOCK, 64),   mat(ModMaterials.BSCCO, MaterialShape.INGOT, 16),
                id("item_secret_folly", 4),                         id("item_secret_controller", 2),                        id("item_secret_folly", 4))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));

        register(new PedestalRecipe(gun("gun_aberrator"),
                null,                           id("item_secret_aberrator", 1),     null,
                id("item_secret_aberrator", 1), id("part_mechanism_saturnite", 4),  id("item_secret_aberrator", 1),
                null,                           id("item_secret_aberrator", 1),     null).set(1));
        register(new PedestalRecipe(gun("gun_aberrator_eott"),
                id("item_secret_aberrator", 1), id("item_secret_aberrator", 1),     id("item_secret_aberrator", 1),
                id("item_secret_aberrator", 1), id("part_mechanism_saturnite", 16), id("item_secret_aberrator", 1),
                id("item_secret_aberrator", 1), id("item_secret_aberrator", 1),     id("item_secret_aberrator", 1))
                .extra(PedestalExtraCondition.GOOD_KARMA).set(1));

        register(new PedestalRecipe(secret(EnumAmmoSecret.FOLLY_SM, 1),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1),    id("magic_powder"), mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1),
                id("magic_powder"),                                     id("moonstone", 1), id("magic_powder"),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1),    id("magic_powder"), mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));
        register(new PedestalRecipe(secret(EnumAmmoSecret.FOLLY_NUKE, 1),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1),    id("magic_powder"),                                         mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1),
                id("magic_powder"),                                     new PedestalInput(() -> WeaponItems.ammo(EnumAmmo.NUKE_HIGH), 4), id("magic_powder"),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1),    id("magic_powder"),                                         mat(ModMaterials.STARMETAL, MaterialShape.INGOT, 1))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));
        register(new PedestalRecipe(secret(EnumAmmoSecret.P35_800, 5),
                null,   null,                               null,
                null,   id("item_secret_aberrator", 1),     null,
                null,   null,                               null).set(1));
        register(new PedestalRecipe(secret(EnumAmmoSecret.P35_800_BL, 10),
                null,   null,                               null,
                null,   id("item_secret_aberrator", 3),     null,
                null,   null,                               null).set(1));
    }

    public static void register(PedestalRecipe recipe) {
        recipes.add(recipe);
        int set = Math.abs(recipe.recipeSet) % recipeSets.length;
        recipeSets[set].add(recipe);
    }

    public static void deleteRecipes() {
        recipes.clear();
        for (int i = 0; i < recipeSets.length; i++) recipeSets[i].clear();
    }

    // ─── Hilfen ──────────────────────────────────────────────────────────────

    private static Supplier<ItemStack> gun(String name) {
        return () -> new ItemStack(WeaponItems.gun(name));
    }

    private static Supplier<ItemStack> secret(EnumAmmoSecret type, int count) {
        return () -> new ItemStack(WeaponItems.ammo(type), count);
    }

    private static PedestalInput gunIn(String name) {
        return new PedestalInput(() -> WeaponItems.gun(name), 1);
    }

    private static PedestalInput mod(EnumModSpecial type) {
        return new PedestalInput(() -> WeaponItems.WEAPON_MOD_SPECIAL.get(type).get(), 1);
    }

    private static PedestalInput mat(ModMaterials mat, MaterialShape shape) {
        return mat(mat, shape, 1);
    }

    /** Original {@code OreDictStack(X.shape(), n)}. */
    private static PedestalInput mat(ModMaterials mat, MaterialShape shape, int count) {
        return new PedestalInput(() -> ModMaterialItems.item(mat, shape), count);
    }

    private static PedestalInput id(String name) {
        return id(name, 1);
    }

    /** Original {@code ComparableStack(ModItems.x / ModBlocks.x, n)} ueber die Registry-ID des Ports. */
    private static PedestalInput id(String name, int count) {
        return new PedestalInput(() -> {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
            return item == Items.AIR ? null : item;
        }, count);
    }

    // ─── Typen ───────────────────────────────────────────────────────────────

    public static enum PedestalExtraCondition {
        NONE, FULL_MOON, NEW_MOON, SUN, GOOD_KARMA, BAD_KARMA
    }

    /** Original {@code AStack}: Gegenstand plus exakte Stapelgroesse. */
    public static class PedestalInput {
        private final Supplier<Item> item;
        public final int stacksize;

        public PedestalInput(Supplier<Item> item, int stacksize) {
            this.item = item;
            this.stacksize = stacksize;
        }

        @Nullable
        public Item getItem() {
            return item.get();
        }

        /** {@code matchesRecipe(stack, true)}: nur der Gegenstand, die Anzahl prueft der Sockel getrennt. */
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            Item it = getItem();
            if (it == null || stack.isEmpty() || !stack.is(it)) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        public ItemStack toStack() {
            Item it = getItem();
            return it == null ? ItemStack.EMPTY : new ItemStack(it, stacksize);
        }
    }

    public static class PedestalRecipe {
        private final Supplier<ItemStack> outputSupplier;
        private ItemStack output;
        public PedestalInput[] input;
        public int recipeSet = 0;
        public PedestalExtraCondition extra = PedestalExtraCondition.NONE;

        public PedestalRecipe(Supplier<ItemStack> output, PedestalInput... input) {
            this.outputSupplier = output;
            this.input = input;
        }

        public ItemStack getOutput() {
            if (output == null) output = outputSupplier.get();
            return output;
        }

        public PedestalRecipe extra(PedestalExtraCondition extra) {
            this.extra = extra;
            return this;
        }

        public PedestalRecipe set(int set) {
            this.recipeSet = set;
            return this;
        }
    }
}
