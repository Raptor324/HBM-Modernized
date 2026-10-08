package com.hbm_m.handler;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.block.decorations.TrinketTypes.PlushieType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.TrinketBlockItem;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.special.ItemKitCustom;
import com.hbm_m.item.special.ItemKitNBT;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 {@code BobmazonOfferFactory} samt {@code GUIScreenBobmazon.Offer}/{@code Requirement}: die Angebote des
 * Bobmazon-Katalogs ({@code standard}) und des versteckten Katalogs ({@code special}). Beide Seiten bauen dieselbe
 * Liste, das Bestellpaket uebertraegt nur den Index.
 *
 * <p>{@code crucible} (alte Nahkampfwaffe) heisst im Port {@code crucible_sword}, weil "crucible" der Giesstiegel ist.</p>
 */
public final class BobmazonOfferFactory {

    private BobmazonOfferFactory() {}

    public static final List<Offer> standard = new ArrayList<>();
    public static final List<Offer> special = new ArrayList<>();

    /** Original: bei jedem WorldEvent.Load; hier einmalig beim ersten Zugriff, wenn alle Register stehen. */
    private static boolean initialized = false;

    public static synchronized void init() {

        standard.clear();
        special.clear();

        //gear
        standard.add(new Offer(new ItemStack(Blocks.TORCH, 64), Requirement.NONE, 2));
        standard.add(new Offer(new ItemStack(ModItems.DEFINITELYFOOD.get(), 16), Requirement.NONE, 4));
        standard.add(new Offer(st("nitra", 4), Requirement.CHEMICS, 16));
        standard.add(new Offer(st("gun_kit_1", 1), Requirement.ASSEMBLY, 16));
        standard.add(new Offer(st("geiger_counter", 1), Requirement.NONE, 16));
        standard.add(new Offer(st("matchstick", 16), Requirement.STEEL, 2));
        //blueprints
        standard.add(new Offer(new ItemStack(ModItems.BLUEPRINT_FOLDER.get(), 1), Requirement.ASSEMBLY, 64));
        standard.add(new Offer(new ItemStack(ModItems.BLUEPRINT_FOLDER_DISCOVER.get(), 1), Requirement.OIL, 256));
        //vending machines
        standard.add(new Offer(new ItemStack(ModBlocks.VENDING_MACHINE.get(), 1), Requirement.CHEMICS, 64));
        standard.add(new Offer(new ItemStack(ModBlocks.VENDING_MACHINE_SNACKS.get(), 1), Requirement.CHEMICS, 64));
        //plants
        standard.add(new Offer(new ItemStack(Blocks.JUNGLE_SAPLING, 1), Requirement.STEEL, 12, 9));
        standard.add(new Offer(new ItemStack(ModBlocks.PLANT_FLOWER_FOXGLOVE.get(), 1), Requirement.STEEL, 16, 5));
        standard.add(new Offer(new ItemStack(ModBlocks.PLANT_FLOWER_TOBACCO.get(), 1), Requirement.STEEL, 16, 9));
        standard.add(new Offer(new ItemStack(ModBlocks.PLANT_FLOWER_NIGHTSHADE.get(), 1), Requirement.STEEL, 16, 3));
        standard.add(new Offer(new ItemStack(ModBlocks.PLANT_FLOWER_WEED.get(), 1), Requirement.STEEL, 4, 10));
        standard.add(new Offer(new ItemStack(ModBlocks.PLANT_FLOWER_CD0.get(), 1), Requirement.NUCLEAR, 64, 8));
        //deco (EnumConcreteType: MACHINE, MACHINE_STRIPE, INDIGO, PURPLE, PINK, HAZARD, SAND, BRONZE)
        for (String conc : new String[] { "machine", "machine_stripe", "indigo", "purple", "pink", "hazard", "sand", "bronze" })
            standard.add(new Offer(st("concrete_colored_ext_" + conc, 16), Requirement.CHEMICS, 4));
        for (TrinketTypes.SnowglobeType globe : TrinketTypes.SnowglobeType.values())
            standard.add(new Offer(TrinketBlockItem.make(ModBlocks.SNOWGLOBE.get().asItem(), globe.ordinal()), Requirement.CHEMICS, 128));
        for (int i = 1; i < PlushieType.values().length; i++)
            standard.add(new Offer(TrinketBlockItem.make(ModBlocks.PLUSHIE.get().asItem(), i), Requirement.OIL, 16, i < 3 ? 10 : 0));

        special.add(new Offer(new ItemStack(Items.IRON_INGOT, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.STEEL, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.COPPER, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.RED_COPPER, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.TITANIUM, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.TUNGSTEN, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.COBALT, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.DESH, 64), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.TANTALIUM, 64), Requirement.STEEL, 5));
        special.add(new Offer(ingot(ModMaterials.BISMUTH, 16), Requirement.STEEL, 5));
        special.add(new Offer(ingot(ModMaterials.SCHRABIDIUM, 16), Requirement.STEEL, 5));
        special.add(new Offer(ingot(ModMaterials.EUPHEMIUM, 8), Requirement.STEEL, 16));
        special.add(new Offer(ingot(ModMaterials.DINEUTRONIUM, 1), Requirement.STEEL, 16));
        special.add(new Offer(ingot(ModMaterials.STARMETAL, 16), Requirement.STEEL, 8));
        special.add(new Offer(ingot(ModMaterials.SEMTEX, 16), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.URANIUM235, 16), Requirement.STEEL, 1));
        special.add(new Offer(ingot(ModMaterials.PLUTONIUM239, 16), Requirement.STEEL, 1));
        special.add(new Offer(st("ammo_container", 16), Requirement.STEEL, 5));
        special.add(new Offer(st("nuke_starter_kit", 1), Requirement.STEEL, 5));
        special.add(new Offer(st("nuke_advanced_kit", 1), Requirement.STEEL, 5));
        special.add(new Offer(st("nuke_commercially_kit", 1), Requirement.STEEL, 5));
        special.add(new Offer(st("boy_kit", 1), Requirement.STEEL, 5));
        special.add(new Offer(st("prototype_kit", 1), Requirement.STEEL, 10));
        special.add(new Offer(st("missile_kit", 1), Requirement.STEEL, 5));
        special.add(new Offer(st("jetpack_vector", 1), Requirement.STEEL, 2));
        special.add(new Offer(st("jetpack_tank", 1), Requirement.STEEL, 2));
        special.add(new Offer(st("gun_kit_1", 1), Requirement.STEEL, 1));
        special.add(new Offer(st("gun_kit_2", 1), Requirement.STEEL, 3));
        special.add(new Offer(st("struct_launcher_core", 1), Requirement.STEEL, 3));
        special.add(new Offer(st("struct_launcher_core_large", 1), Requirement.STEEL, 3));
        special.add(new Offer(st("struct_launcher", 40), Requirement.STEEL, 7));
        special.add(new Offer(st("struct_scaffold", 11), Requirement.STEEL, 7));
        special.add(new Offer(st("loot_10", 1), Requirement.STEEL, 2));
        special.add(new Offer(st("loot_15", 1), Requirement.STEEL, 2));
        special.add(new Offer(st("loot_misc", 1), Requirement.STEEL, 2));
        special.add(new Offer(st("crate_can", 1), Requirement.STEEL, 1));
        special.add(new Offer(st("crate_ammo", 1), Requirement.STEEL, 2));
        // Original new ItemStack(ModItems.crucible, 1, 3): Klinge mit Schaden 3 = ohne Ladung
        ItemStack crucible = new ItemStack(com.hbm_m.item.ModItems.CRUCIBLE_SWORD.get());
        crucible.setDamageValue(3);
        special.add(new Offer(crucible, Requirement.STEEL, 10));
        special.add(new Offer(st("chopper", 1), Requirement.STEEL, 10));
        special.add(new Offer(st("spawn_worm", 1), Requirement.STEEL, 10));
        special.add(new Offer(st("spawn_ufo", 1), Requirement.STEEL, 10));
        special.add(new Offer(st("sat_laser", 1), Requirement.HIDDEN, 8));
        special.add(new Offer(st("sat_gerald", 1), Requirement.HIDDEN, 32));
        special.add(new Offer(new ItemStack(ModMaterialItems.item(ModMaterials.YHARONITE, MaterialShape.BILLET), 4), Requirement.HIDDEN, 16));
        special.add(new Offer(ingot(ModMaterials.CHAINSSTEEL, 1), Requirement.HIDDEN, 16));
        special.add(new Offer(ingot(ModMaterials.ELECTRONIUM, 1), Requirement.HIDDEN, 16));
        special.add(new Offer(st("book_of_", 1), Requirement.HIDDEN, 16));
        special.add(new Offer(st("mese_pickaxe", 1), Requirement.HIDDEN, 16));
        special.add(new Offer(st("mysteryshovel", 1), Requirement.HIDDEN, 16));
        special.add(new Offer(new ItemStack(ModBlocks.NTM_DIRT.get(), 1), Requirement.HIDDEN, 16));
        special.add(new Offer(st("euphemium_kit", 1), Requirement.HIDDEN, 64));

        ItemStack spark = st("battery_spark", 1);
        if (spark.getItem() instanceof ModBatteryItem battery) ModBatteryItem.setEnergy(spark, battery.getCapacity());

        special.add(new Offer(ItemKitCustom.create("Fusion Man", "For the nuclear physicist on the go", 0xff00ff, 0x800080,
                st("klystron", 1),
                st("torus", 1),
                st("mhdt", 1),
                st("intake", 3),
                spark,
                st("chemical_factory", 4),
                st("fluid_tank", 8),
                st("red_wire_coated", 64),
                st("red_cable", 64),
                ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DEUTERIUM.getSource(), 64),
                ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.TRITIUM.getSource(), 64),
                ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PERFLUOROMETHYL.getSource(), 64),
                st("red_pylon_large", 8),
                st("substation", 4),
                st("red_connector", 64),
                st("wiring_red_copper", 1),
                st("machine_chungus", 3),
                st("template_folder", 1),
                new ItemStack(Items.PAPER, 64),
                new ItemStack(Items.INK_SAC, 64)
                ), Requirement.HIDDEN, 64));

        special.add(new Offer(ItemKitCustom.create("Maid's Cleaning Utensils", "For the hard to reach spots", 0x00ff00, 0x008000,
                gun("gun_m2"),
                ammo(EnumAmmo.BMG50_DU, 64),
                ammo(EnumAmmo.BMG50_DU, 64),
                ammo(EnumAmmo.BMG50_DU, 64),
                ammo(EnumAmmo.BMG50_DU, 64),
                ammo(EnumAmmo.BMG50_DU, 64),
                gun("gun_autoshotgun"),
                ammo(EnumAmmo.G12_MAGNUM, 64),
                ammo(EnumAmmo.G12_MAGNUM, 64),
                ammo(EnumAmmo.G12_MAGNUM, 64),
                ammo(EnumAmmo.G12_EXPLOSIVE, 64),
                ammo(EnumAmmo.G12_EXPLOSIVE, 64)
                ), Requirement.HIDDEN, 64));

        special.add(new Offer(named(ItemKitNBT.create(
                named(st("rod_of_discord", 1), "Cock Joke"),
                named(st("canned_slime", 64), "Class A Horse Semen"),
                named(st("pipe_lead", 1), "Get Nutted, Dumbass"),
                new ItemStack(ModItems.GEM_ALEXANDRITE.get())
                ), "The Nut Bucket"), Requirement.HIDDEN, 64));

        special.add(new Offer(named(ItemKitNBT.create(
                new ItemStack(ModItems.RPA_HELMET.get()),
                new ItemStack(ModItems.RPA_PLATE.get()),
                new ItemStack(ModItems.RPA_LEGS.get()),
                new ItemStack(ModItems.RPA_BOOTS.get()),
                gun("gun_minigun_lacunae"),
                ammo(EnumAmmo.CAPACITOR_OVERCHARGE, 64),
                ammo(EnumAmmo.CAPACITOR_OVERCHARGE, 64),
                ammo(EnumAmmo.CAPACITOR_OVERCHARGE, 64)
                ), "Frenchie's Reward"), Requirement.HIDDEN, 32));

        initialized = true;
    }

    @Nullable
    public static List<Offer> getOffers(ItemStack stack) {

        if (!initialized) init();

        if (!stack.isEmpty()) {
            if (stack.is(ModItems.BOBMAZON.get())) return standard;
            if (stack.is(ModItems.BOBMAZON_HIDDEN.get())) return special;
        }

        return null;
    }

    private static ItemStack named(ItemStack stack, String name) {
        StackNbt.setCustomName(stack, Component.literal(name));
        return stack;
    }

    /** Original {@code new ItemStack(ModItems.gun_x)}. */
    private static ItemStack gun(String name) {
        return new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.gun(name));
    }

    /** Original {@code new ItemStack(ModItems.ammo_standard, count, EnumAmmo.X.ordinal())}. */
    private static ItemStack ammo(EnumAmmo type, int count) {
        return new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.ammo(type), count);
    }

    private static ItemStack ingot(ModMaterials mat, int count) {
        return new ItemStack(ModMaterialItems.item(mat, MaterialShape.INGOT), count);
    }

    /** Gegenstand nach Registriername (Multiblock-Gegenstaende haben keine eigene Konstante je Block). */
    private static ItemStack st(String name, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
        if (item == Items.AIR) MainRegistry.LOGGER.warn("[Bobmazon] unbekannter Gegenstand {}", name);
        return new ItemStack(item, count);
    }

    /** {@code GUIScreenBobmazon.Offer}. */
    public static class Offer {

        public final ItemStack offer;
        public final Requirement requirement;
        public final int cost;
        public final int rating;
        public final String comment;
        public final String author;

        public Offer(ItemStack offer, Requirement requirement, int cost, int rating, String comment, String author) {
            this.offer = offer;
            this.requirement = requirement;
            this.cost = cost;
            this.rating = rating * 4 - 1;
            this.comment = comment;
            this.author = author;
        }

        public Offer(ItemStack offer, Requirement requirement, int cost) {
            this(offer, requirement, cost, 0);
        }

        public Offer(ItemStack offer, Requirement requirement, int cost, int rating) {
            this(offer, requirement, cost, rating, "No Ratings", "");
        }
    }

    /**
     * {@code GUIScreenBobmazon.Requirement}: die Erfolge des Originals als Fortschritte. NONE war
     * {@code openInventory}, hier der Wurzelfortschritt (erster Tick in der Welt). Das Symbol ist das des
     * Originalerfolgs.
     */
    public enum Requirement {

        NONE("root", () -> new ItemStack(Items.BOOK)),
        STEEL(ModAdvancements.BLAST_FURNACE, () -> st("machine_blast_furnace", 1)),
        ASSEMBLY(ModAdvancements.ASSEMBLY, () -> st("machine_assembler", 1)),
        CHEMICS(ModAdvancements.CHEMPLANT, () -> st("chemical_plant", 1)),
        OIL(ModAdvancements.DESH, () -> ingot(ModMaterials.DESH, 1)),
        NUCLEAR(ModAdvancements.TECHNETIUM, () -> ingot(ModMaterials.TCALLOY, 1)),
        HIDDEN(ModAdvancements.HIDDEN, () -> new ItemStack(ModItems.NOTHING.get()));

        public final String advancement;
        private final Supplier<ItemStack> icon;
        private ItemStack iconCache;

        Requirement(String advancement, Supplier<ItemStack> icon) {
            this.advancement = advancement;
            this.icon = icon;
        }

        public ItemStack getIcon() {
            if (iconCache == null) iconCache = icon.get();
            return iconCache;
        }

        public boolean fullfills(net.minecraft.server.level.ServerPlayer player) {
            //? if < 1.21.1 {
            var adv = player.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, advancement));
            //?} else {
            /*var adv = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, advancement));
            *///?}
            return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
        }
    }
}
