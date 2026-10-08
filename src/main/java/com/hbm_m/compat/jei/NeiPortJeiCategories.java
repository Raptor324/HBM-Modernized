package com.hbm_m.compat.jei;
//? if forge {

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Entry;
import com.hbm_m.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Die NEI-Handler des Originals ({@code NEIRegistry.listAllHandlers}), die der Port bis dahin
 * nicht im Rezeptbrowser hatte. Sammelt Kategorien, Rezepte, Maschinen (Katalysatoren) und
 * Klickflaechen an einer Stelle, damit {@link HbmJeiPlugin} nur je einen Aufruf braucht.
 */
public final class NeiPortJeiCategories {

    private NeiPortJeiCategories() {}

    /** Eine {@code NEIUniversalHandler}-Unterklasse: ID, Titel, Maschinen und Rezeptquelle. */
    private record Universal(RecipeType<Entry> type, String titleKey, String fallback, List<ItemStack> machines,
                             Function<Level, List<Entry>> recipes) { }

    private static final List<Universal> UNIVERSAL = new ArrayList<>();
    private static final List<CustomMachineJeiCategory> CUSTOM = new ArrayList<>();

    public static final RecipeType<Entry> GRENADE = type("grenade_crafting");

    private static RecipeType<Entry> type(String id) {
        return RecipeType.create(RefStrings.MODID, id, Entry.class);
    }

    private static List<ItemStack> stacks(ItemStack... s) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack st : s) if (st != null && !st.isEmpty()) list.add(st);
        return list;
    }

    private static ItemStack it(String id) {
        return NeiUniversalJeiRecipes.it(id);
    }

    /** Reihenfolge wie {@code NEIRegistry}; {@code fallback == null} = Name der ersten Maschine (getLocalizedName). */
    private static void build() {
        if (!UNIVERSAL.isEmpty()) return;
        add("tooling", "Tooling", stacks(new ItemStack(ModItems.BOLTGUN.get()), new ItemStack(ModItems.BLOWTORCH.get()), new ItemStack(ModItems.ACETYLENE_TORCH.get())),
                l -> NeiUniversalJeiRecipes.tooling());
        add("construction", "Construction", stacks(new ItemStack(ModItems.ACETYLENE_TORCH.get()), new ItemStack(ModItems.BLOWTORCH.get()), new ItemStack(ModItems.BOLTGUN.get())),
                l -> NeiUniversalJeiRecipes.construction());
        add("annihilator", "Annihilator", stacks(new ItemStack(ModBlocks.ANNIHILATOR.get())), l -> NeiUniversalJeiRecipes.annihilator());
        add("ore_slopper", null, stacks(new ItemStack(ModBlocks.ORE_SLOPPER.get())), l -> NeiUniversalJeiRecipes.oreSlopper());
        add("spent_fuel_pool", "Spent Fuel Pool Drum", stacks(new ItemStack(ModBlocks.MACHINE_WASTE_DRUM.get())), l -> NeiUniversalJeiRecipes.fuelPool());
        add("grenade_crafting", "Grenade Crafting", stacks(new ItemStack(Items.CRAFTING_TABLE)), l -> NeiUniversalJeiRecipes.grenades());

        // "universal boyes"
        add("zirnox", null, stacks(it("zirnox")), l -> NeiUniversalJeiRecipes.zirnox());
        add("pwr", null, stacks(new ItemStack(ModBlocks.PWR_CONTROLLER.get())), l -> NeiUniversalJeiRecipes.pwr());
        add("watz", null, stacks(new ItemStack(ModItems.WATZ_POWERPLANT.get())), l -> NeiUniversalJeiRecipes.watz());
        if (com.hbm_m.config.VersatileConfig.rtgDecay()) {
            add("rtg", "RTG", stacks(it("machine_rtg"), it("machine_difurnace_rtg_off")), l -> NeiUniversalJeiRecipes.rtg());
        }
        add("boiling", null, stacks(it("boiler"), it("industrial_boiler")), l -> NeiUniversalJeiRecipes.boiling());
        add("combination", null, stacks(it("combination_oven")), NeiUniversalJeiRecipes::combination);
        add("sawmill", null, stacks(it("sawmill")), l -> NeiUniversalJeiRecipes.sawmill());
        add("mixer", null, stacks(it("mixer")), l -> NeiUniversalJeiRecipes.mixer());
        add("fusion_breeder", null, stacks(new ItemStack(ModItems.FUSION_BREEDER_ITEM.get())), l -> NeiUniversalJeiRecipes.fusionBreeder());
        add("ashpit", null, stacks(it("ashpit")), l -> NeiUniversalJeiRecipes.ashpit());
        add("deuterium", null, stacks(it("machine_deuterium_extractor"), it("deuterium_tower")), l -> NeiUniversalJeiRecipes.deuterium());
    }

    private static void add(String id, String fallback, List<ItemStack> machines, Function<Level, List<Entry>> recipes) {
        if (machines.isEmpty()) return;
        RecipeType<Entry> type = id.equals("grenade_crafting") ? GRENADE : type(id);
        UNIVERSAL.add(new Universal(type, "jei.hbm_m." + id, fallback, machines, recipes));
    }

    private static Component title(Universal u) {
        if (u.fallback() == null) return u.machines().get(0).getHoverName();
        return Component.translatableWithFallback(u.titleKey(), u.fallback());
    }

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        build();
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (Universal u : UNIVERSAL) {
            registration.addRecipeCategories(new NeiUniversalJeiCategory(gui, u.type(), title(u), u.machines().toArray(new ItemStack[0])));
        }
        registration.addRecipeCategories(new RefineryJeiCategory(gui));
        registration.addRecipeCategories(new BookJeiCategory(gui));
        registration.addRecipeCategories(new SatelliteJeiCategory(gui));
        registration.addRecipeCategories(new FluidContainerJeiCategory(gui));

        // NEIConfig: fuer jede Custom Machine ein eigener CustomMachineHandler
        CUSTOM.clear();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MachineConfiguration conf : CustomMachineJeiCategory.configurations()) {
            CustomMachineJeiCategory cat = new CustomMachineJeiCategory(gui, conf);
            if (!seen.add(cat.getRecipeType().getUid().toString())) continue;
            CUSTOM.add(cat);
            registration.addRecipeCategories(cat);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration, Level level) {
        build();
        for (Universal u : UNIVERSAL) {
            List<Entry> list;
            try {
                list = u.recipes().apply(level);
            } catch (RuntimeException ex) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[hbm_m] JEI: Rezepte fuer {} nicht ladbar", u.type().getUid(), ex);
                continue;
            }
            registration.addRecipes(u.type(), list);
        }
        registration.addRecipes(RefineryJeiCategory.RECIPE_TYPE, RefineryJeiCategory.recipes());
        registration.addRecipes(BookJeiCategory.RECIPE_TYPE, BookJeiCategory.recipes());
        registration.addRecipes(SatelliteJeiCategory.RECIPE_TYPE, SatelliteJeiCategory.recipes());
        registration.addRecipes(FluidContainerJeiCategory.RECIPE_TYPE, FluidContainerJeiCategory.recipes());
        for (CustomMachineJeiCategory cat : CUSTOM) {
            registration.addRecipes(cat.getRecipeType(), CustomMachineJeiCategory.recipes(cat.getConfiguration()));
        }
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        build();
        for (Universal u : UNIVERSAL) {
            for (ItemStack machine : u.machines()) registration.addRecipeCatalyst(machine, u.type());
        }
        registration.addRecipeCatalyst(new ItemStack(ModItems.REFINERY.get()), RefineryJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.BOOK_OF_.get()), BookJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SAT_DOCK.get()), SatelliteJeiCategory.RECIPE_TYPE);
        for (ItemStack con : FluidContainerJeiCategory.machines()) registration.addRecipeCatalyst(con, FluidContainerJeiCategory.RECIPE_TYPE);
        for (CustomMachineJeiCategory cat : CUSTOM) registration.addRecipeCatalyst(cat.getMachine(), cat.getRecipeType());

        // Maschinen bestehender Kategorien, die das Original zusaetzlich nennt.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COMPRESSOR_COMPACT.get()), CompressorJeiCategory.RECIPE_TYPE);   // CompressorHandler
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RBMK_OUTGASSER.get()), RBMKOutgasserJeiCategory.RECIPE_TYPE);    // OutgasserHandler
        registration.addRecipeCatalyst(new ItemStack(ModItems.FUSION_BREEDER_ITEM.get()), RBMKOutgasserJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), RBMKDisassemblyJeiCategory.RECIPE_TYPE);           // RBMKRodDisassemblyHandler
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()), RBMKWasteDecayJeiCategory.RECIPE_TYPE); // RBMKWasteDecayHandler
    }

    /** Transfer-Rechtecke der Original-Handler (NEI-Koordinaten der GUI). */
    public static void registerGuiHandlers(IGuiHandlerRegistration registration) {
        build();
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineCombinationOven.class, 49, 44, 18, 18, type("combination"));
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineMixer.class, 66, 20, 52, 44, type("mixer"));
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineRefinery.class, 34, 34, 14, 34, RefineryJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIBook.class, 89, 34, 24, 18, BookJeiCategory.RECIPE_TYPE);
    }

    /** Baukastengranaten unterscheiden sich nur im NBT - fuer "Rezept zu genau dieser Granate". */
    public static void registerSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(com.hbm_m.item.weapon.grenade.GrenadeItems.GRENADE_UNIVERSAL.get(), (stack, ctx) -> {
            var shell = com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal.getShell(stack);
            var filling = com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal.getFilling(stack);
            var fuze = com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal.getFuze(stack);
            var extra = com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal.getExtra(stack);
            return shell + ";" + filling + ";" + fuze + ";" + extra;
        });
    }
}
//?} else {
/*public final class NeiPortJeiCategories {
    private NeiPortJeiCategories() {}
}*///?}
