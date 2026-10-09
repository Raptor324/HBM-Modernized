package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.hbm_m.annihilator.AnnihilatorPoolManager;
import com.hbm_m.annihilator.AnnihilatorRecipes;
import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.fusion.MachineFusionTorusBlock;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Entry;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Slot;
import com.hbm_m.handler.fusion.FluidBreederRecipes;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.inventory.recipes.FuelPoolRecipes;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.BedrockOreType;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.nuclear.PWRFuelType;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.weapon.grenade.GrenadeItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CombinationOvenRecipe;
import com.hbm_m.recipe.MixerRecipes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Rezeptlisten der einfachen {@code NEIUniversalHandler}-Unterklassen des Originals, je Methode
 * eine Klasse aus {@code com.hbm.handler.nei}. Reihenfolge, Mengen und Maschinen wie dort.
 */
public final class NeiUniversalJeiRecipes {

    private NeiUniversalJeiRecipes() {}

    // ---- Hilfen ----

    static ItemStack it(String id) {
        return it(id, 1);
    }

    /** Port-Gegenstand ueber die ID; fehlt er, ein leerer Stack (das Feld faellt dann weg). */
    static ItemStack it(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        return stack;
    }

    static ItemStack it(Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        return stack;
    }

    private static ItemStack mat(ModMaterials m, MaterialShape shape, int count) {
        return ModMaterialItems.stack(m, shape, count);
    }

    /** Leere Felder entfernen (Gegenstand im Port nicht vorhanden). */
    private static List<Slot> clean(Slot... slots) {
        List<Slot> list = new ArrayList<>();
        for (Slot s : slots) {
            if (s == null) continue;
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack st : s.items()) if (!st.isEmpty()) items.add(st);
            if (s.fluid() == null && items.isEmpty()) continue;
            list.add(new Slot(items, s.fluid(), s.amount(), s.tooltip()));
        }
        return list;
    }

    private static Component red(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    private static List<ItemStack> items(Ingredient ingredient, int count) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : ingredient.getItems()) {
            ItemStack c = s.copy();
            c.setCount(count);
            list.add(c);
        }
        return list;
    }

    // ---- ToolingHandler / BlockToolConversion.getRecipes ----

    public static List<Entry> tooling() {
        List<ItemStack> bolt = List.of(new ItemStack(ModItems.BOLTGUN.get()));
        List<ItemStack> torch = List.of(new ItemStack(ModItems.BLOWTORCH.get()), new ItemStack(ModItems.ACETYLENE_TORCH.get()));
        List<Entry> list = new ArrayList<>();
        // BOLT watz_end:0 + 4x DURA.bolt -> watz_end:1
        list.add(new Entry(clean(Slot.of(it(ModItems.BOLT_HIGHSPEED_STEEL.get(), 4)), Slot.of(new ItemStack(ModBlocks.WATZ_END.get()))),
                List.of(Slot.of(new ItemStack(ModBlocks.WATZ_END_BOLTED.get()))), bolt));
        // TORCH fusion_component:0 + STEEL.plateCast -> fusion_component:1
        list.add(new Entry(clean(Slot.of(mat(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 1)), Slot.of(new ItemStack(ModBlocks.FUSION_COMPONENT.get()))),
                List.of(Slot.of(new ItemStack(ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get()))), torch));
        // TORCH icf_component:1 + ANY_BISMOIDBRONZE.plateCast -> icf_component:2
        list.add(new Entry(clean(Slot.of(mat(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST, 1), mat(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST, 1)),
                Slot.of(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL.get()))),
                List.of(Slot.of(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get()))), torch));
        // BOLT icf_component:3 + STEEL.plateCast + 4x DURA.bolt -> icf_component:4
        list.add(new Entry(clean(Slot.of(mat(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 1)), Slot.of(it(ModItems.BOLT_HIGHSPEED_STEEL.get(), 4)),
                Slot.of(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE.get()))),
                List.of(Slot.of(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get()))), bolt));
        return list;
    }

    // ---- ConstructionHandler ----

    public static List<Entry> construction() {
        List<Entry> list = new ArrayList<>();
        Item duraBolt = ModItems.BOLT_HIGHSPEED_STEEL.get();

        /* WATZ */
        list.add(new Entry(clean(
                Slot.of(it(ModBlocks.WATZ_END.get().asItem(), 48)),
                Slot.of(it(duraBolt, 64)), Slot.of(it(duraBolt, 64)), Slot.of(it(duraBolt, 64)),
                Slot.of(it(ModBlocks.WATZ_ELEMENT.get().asItem(), 36)),
                Slot.of(it(ModBlocks.WATZ_COOLER.get().asItem(), 26)),
                Slot.of(new ItemStack(ModItems.BOLTGUN.get()))),
                List.of(Slot.of(new ItemStack(ModItems.WATZ_POWERPLANT.get()))),
                List.of(new ItemStack(ModBlocks.STRUCT_WATZ_CORE.get()))));

        /* COMPACT LAUNCHER */
        list.add(new Entry(List.of(Slot.of(it(ModBlocks.STRUCT_LAUNCHER.get().asItem(), 8))),
                List.of(Slot.of(new ItemStack(ModBlocks.COMPACT_LAUNCHER.get()))),
                List.of(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE.get()))));

        /* LAUNCH TABLE */
        list.add(new Entry(List.of(
                Slot.of(it(ModBlocks.STRUCT_LAUNCHER.get().asItem(), 16)),
                Slot.of(it(ModBlocks.STRUCT_LAUNCHER.get().asItem(), 64)),
                Slot.of(it(ModBlocks.STRUCT_SCAFFOLD.get().asItem(), 11))),
                List.of(Slot.of(new ItemStack(ModBlocks.LAUNCH_TABLE.get()))),
                List.of(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()))));

        /* SOYUZ LAUNCHER */
        list.add(new Entry(List.of(
                Slot.of(it(ModBlocks.STRUCT_LAUNCHER.get().asItem(), 30)),
                Slot.of(it(ModBlocks.STRUCT_LAUNCHER.get().asItem(), 384)),
                Slot.of(it(ModBlocks.STRUCT_SCAFFOLD.get().asItem(), 63)),
                Slot.of(it(ModBlocks.STRUCT_SCAFFOLD.get().asItem(), 384)),
                Slot.of(it(ModBlocks.CONCRETE_SMOOTH.get().asItem(), 38)),
                Slot.of(it(ModBlocks.CONCRETE_SMOOTH.get().asItem(), 320))),
                List.of(Slot.of(new ItemStack(ModItems.SOYUZ_LAUNCHER.get()))),
                List.of(new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get()))));

        /* ICF */
        list.add(new Entry(clean(
                Slot.of(it(ModBlocks.ICF_COMPONENT.get().asItem(), 50)),
                Slot.of(it(ModBlocks.ICF_COMPONENT_STRUCTURE.get().asItem(), 240)),
                Slot.of(it(duraBolt, 960)),
                Slot.of(mat(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 240)),
                Slot.of(it(ModBlocks.ICF_COMPONENT_VESSEL.get().asItem(), 117)),
                Slot.of(mat(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST, 117), mat(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST, 117)),
                Slot.of(new ItemStack(ModItems.BLOWTORCH.get())),
                Slot.of(new ItemStack(ModItems.BOLTGUN.get()))),
                List.of(Slot.of(new ItemStack(ModBlocks.ICF.get()))),
                List.of(new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()))));

        /* FUSION TORUS */
        int wallCount = 0;
        int blanketCount = 0;
        int pipeCount = -1; // one block is replaced by the core
        for (int iy = 0; iy < 5; iy++) {
            int l = iy > 2 ? 4 - iy : iy;
            int[][] layer = MachineFusionTorusBlock.LAYOUT[l];
            for (int ix = 0; ix < layer.length; ix++) for (int iz = 0; iz < layer.length; iz++) {
                int meta = layer[ix][iz];
                if (meta == 1) wallCount++;
                if (meta == 2) blanketCount++;
                if (meta == 3) pipeCount++;
            }
        }
        List<Slot> torus = new ArrayList<>();
        int plateCount = wallCount;
        while (wallCount > 0) { int a = Math.min(wallCount, 256); torus.add(Slot.of(it(ModBlocks.FUSION_COMPONENT.get().asItem(), a))); wallCount -= a; }
        while (plateCount > 0) { int a = Math.min(plateCount, 256); torus.add(Slot.of(mat(ModMaterials.STEEL, MaterialShape.PLATE_CAST, a))); plateCount -= a; }
        while (blanketCount > 0) { int a = Math.min(blanketCount, 256); torus.add(Slot.of(it(ModBlocks.FUSION_COMPONENT_BLANKET.get().asItem(), a))); blanketCount -= a; }
        while (pipeCount > 0) { int a = Math.min(pipeCount, 256); torus.add(Slot.of(it(ModBlocks.FUSION_COMPONENT_MOTOR.get().asItem(), a))); pipeCount -= a; }
        torus.add(Slot.of(new ItemStack(ModItems.BLOWTORCH.get())));
        list.add(new Entry(clean(torus.toArray(new Slot[0])),
                List.of(Slot.of(new ItemStack(ModItems.FUSION_TORUS_ITEM.get()))),
                List.of(new ItemStack(ModBlocks.STRUCT_TORUS_CORE.get()))));

        return list;
    }

    // ---- AnnihilatorHandler / AnnihilatorRecipes.getRecipes ----

    public static List<Entry> annihilator() {
        AnnihilatorRecipes.registerDefaults();
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, List<AnnihilatorRecipes.Milestone>> e : AnnihilatorRecipes.recipes.entrySet()) {
            String key = e.getKey();
            for (AnnihilatorRecipes.Milestone milestone : e.getValue()) {
                Component amount = red(String.format(Locale.US, "%,d", milestone.amount()));
                Slot in = keySlot(key);
                if (in == null) continue;
                ItemStack payout = milestone.payout().get();
                if (payout == null || payout.isEmpty()) continue;
                list.add(new Entry(List.of(in.tip(amount)), List.of(Slot.of(payout.copy()))));
            }
        }
        return list;
    }

    private static Slot keySlot(String key) {
        int colon = key.indexOf(':');
        if (colon < 0) return null;
        String kind = key.substring(0, colon);
        String id = key.substring(colon + 1);
        switch (kind) {
            case "item", "comp" -> {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
                return item == Items.AIR ? null : Slot.of(new ItemStack(item));
            }
            case "fluid" -> {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(id));
                return fluid == Fluids.EMPTY ? null : Slot.fluid(fluid, 0);
            }
            case "dict" -> {
                List<ItemStack> stacks = new ArrayList<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item == Items.AIR) continue;
                    ItemStack s = new ItemStack(item);
                    if (AnnihilatorPoolManager.dictNames(s).contains(id)) stacks.add(s);
                }
                return stacks.isEmpty() ? null : Slot.of(stacks);
            }
            default -> { return null; }
        }
    }

    // ---- OreSlopperHandler ----

    public static List<Entry> oreSlopper() {
        List<Slot> outputs = new ArrayList<>();
        for (BedrockOreType type : BedrockOreType.values()) {
            Item item = type.item("base");
            if (item != null) outputs.add(Slot.of(new ItemStack(item)));
        }
        outputs.add(Slot.fluid(ModFluids.SLOP.getSource(), 1000));
        return List.of(new Entry(
                List.of(Slot.fluid(ModFluids.WATER.getSource(), 1000), Slot.of(new ItemStack(ModItems.BEDROCK_ORE_BASE.get()))),
                outputs));
    }

    // ---- FuelPoolHandler ----

    public static List<Entry> fuelPool() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Item, ItemStack> e : FuelPoolRecipes.recipes().entrySet()) {
            list.add(new Entry(List.of(Slot.of(new ItemStack(e.getKey()))), List.of(Slot.of(e.getValue().copy()))));
        }
        return list;
    }

    // ---- ZirnoxRecipeHandler / TileEntityReactorZirnox.fuelMap ----

    public static List<Entry> zirnox() {
        String[][] map = {
                {"rod_zirnox_natural_uranium_fuel", "rod_zirnox_natural_uranium_fuel_depleted"},
                {"rod_zirnox_uranium_fuel", "rod_zirnox_uranium_fuel_depleted"},
                {"rod_zirnox_th232", "rod_zirnox_thorium_fuel"},
                {"rod_zirnox_thorium_fuel", "rod_zirnox_thorium_fuel_depleted"},
                {"rod_zirnox_mox_fuel", "rod_zirnox_mox_fuel_depleted"},
                {"rod_zirnox_plutonium_fuel", "rod_zirnox_plutonium_fuel_depleted"},
                {"rod_zirnox_u233_fuel", "rod_zirnox_u233_fuel_depleted"},
                {"rod_zirnox_u235_fuel", "rod_zirnox_u235_fuel_depleted"},
                {"rod_zirnox_les_fuel", "rod_zirnox_les_fuel_depleted"},
                {"rod_zirnox_lithium", "rod_zirnox_tritium"},
                {"rod_zirnox_zfb_mox", "rod_zirnox_zfb_mox_depleted"},
        };
        return pairs(map);
    }

    private static List<Entry> pairs(String[][] map) {
        List<Entry> list = new ArrayList<>();
        for (String[] pair : map) {
            ItemStack in = it(pair[0]);
            ItemStack out = it(pair[1]);
            if (in.isEmpty() || out.isEmpty()) continue;
            list.add(new Entry(List.of(Slot.of(in)), List.of(Slot.of(out))));
        }
        return list;
    }

    // ---- PWRRecipeHandler ----

    public static List<Entry> pwr() {
        List<String[]> map = new ArrayList<>();
        for (PWRFuelType fuel : PWRFuelType.values()) {
            String name = "pwr_fuel_" + fuel.name().toLowerCase(Locale.ROOT);
            map.add(new String[] {name, name + "_hot"});
        }
        return pairs(map.toArray(new String[0][]));
    }

    // ---- WatzRecipeHandler ----

    public static List<Entry> watz() {
        List<Entry> list = new ArrayList<>();
        for (WatzPelletType fuel : WatzPelletType.values()) {
            var fresh = ModItems.WATZ_PELLET.get(fuel);
            var depleted = ModItems.WATZ_PELLET_DEPLETED.get(fuel);
            if (fresh == null || depleted == null) continue;
            list.add(new Entry(List.of(Slot.of(new ItemStack(fresh.get()))), List.of(Slot.of(new ItemStack(depleted.get())))));
        }
        return list;
    }

    // ---- RTGRecipeHandler / ItemRTGPellet.getRecipeMap ----

    public static List<Entry> rtg() {
        List<Entry> list = new ArrayList<>();
        for (ItemRTGPellet pellet : ItemRTGPellet.PELLETS) {
            ItemStack decay = pellet.getDecayItem();
            if (decay == null || decay.isEmpty()) continue;
            list.add(new Entry(List.of(Slot.of(new ItemStack(pellet))), List.of(Slot.of(decay))));
        }
        return list;
    }

    // ---- BoilingHandler ----

    public static List<Entry> boiling() {
        Set<Fluid> order = new LinkedHashSet<>();
        for (ModFluids.FluidEntry e : HbmFluidRegistry.getOrderedFluids()) order.add(e.getSource());
        for (Fluid f : BuiltInRegistries.FLUID) if (f.isSource(f.defaultFluidState())) order.add(f);

        List<Entry> list = new ArrayList<>();
        for (Fluid type : order) {
            FT_Heatable trait = FluidType.getTrait(type, FT_Heatable.class);
            if (trait == null || trait.getEfficiency(HeatingType.BOILER) <= 0) continue;
            HeatingStep step = trait.getFirstStep();
            if (step == null) continue;
            list.add(new Entry(List.of(Slot.fluid(type, step.amountReq)), List.of(Slot.fluid(step.typeProduced, step.amountProduced))));
        }
        return list;
    }

    // ---- CombinationHandler / CombinationRecipes.getRecipes ----

    public static List<Entry> combination(Level level) {
        List<Entry> list = new ArrayList<>();
        for (CombinationOvenRecipe r : RecipeHooks.getAllRecipes(level, CombinationOvenRecipe.Type.INSTANCE)) {
            List<Slot> outs = new ArrayList<>();
            ItemStack out = r.getOutput();
            if (out != null && !out.isEmpty()) outs.add(Slot.of(out));
            if (r.getOutputFluid() != null && r.getOutputFluid() != Fluids.EMPTY && r.getOutputFluidAmount() > 0) {
                outs.add(Slot.fluid(r.getOutputFluid(), r.getOutputFluidAmount()));
            }
            if (outs.isEmpty()) continue;
            list.add(new Entry(List.of(Slot.of(Arrays.asList(r.getInput().getItems()))), outs));
        }
        return list;
    }

    // ---- SawmillHandler / TileEntitySawmill.getRecipes ----

    public static List<Entry> sawmill() {
        ItemStack sawdust = new ItemStack(ModItems.POWDER_SAWDUST.get());
        List<Entry> list = new ArrayList<>();
        list.add(new Entry(List.of(Slot.of(items(Ingredient.of(ItemTags.LOGS), 1))),
                List.of(Slot.of(new ItemStack(Items.OAK_PLANKS, 6)), Slot.of(sawdust.copy()).tip(red("50%")))));
        list.add(new Entry(List.of(Slot.of(items(Ingredient.of(ItemTags.PLANKS), 1))),
                List.of(Slot.of(new ItemStack(Items.STICK, 6)), Slot.of(sawdust.copy()).tip(red("10%")))));
        list.add(new Entry(List.of(Slot.of(new ItemStack(Items.STICK))), List.of(Slot.of(sawdust.copy()))));
        list.add(new Entry(List.of(Slot.of(items(Ingredient.of(ItemTags.SAPLINGS), 1))),
                List.of(Slot.of(new ItemStack(Items.STICK, 1)), Slot.of(sawdust.copy()).tip(red("10%")))));
        return list;
    }

    // ---- MixerHandler / MixerRecipes.getRecipes ----

    public static List<Entry> mixer() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Fluid, MixerRecipes.MixerRecipe[]> e : MixerRecipes.getRecipes().entrySet()) {
            for (MixerRecipes.MixerRecipe recipe : e.getValue()) {
                List<Slot> ins = new ArrayList<>();
                if (recipe.input1 != null) ins.add(Slot.fluid(recipe.input1.type(), recipe.input1.fill()));
                if (recipe.input2 != null) ins.add(Slot.fluid(recipe.input2.type(), recipe.input2.fill()));
                if (recipe.solidInput != null) {
                    List<ItemStack> solids = new ArrayList<>();
                    for (ItemStack s : recipe.solidInput.display().get()) {
                        ItemStack c = s.copy();
                        c.setCount(recipe.solidInput.stacksize());
                        solids.add(c);
                    }
                    ins.add(Slot.of(solids));
                }
                list.add(new Entry(ins, List.of(Slot.fluid(e.getKey(), recipe.output))));
            }
        }
        return list;
    }

    // ---- AshpitHandler ----

    public static List<Entry> ashpit() {
        Slot ovens = Slot.of(it("firebox"), it("heating_oven"));
        Slot chimneys = Slot.of(it("chimney_brick"), it("chimney_industrial"));
        Slot coals = Slot.of(new ItemStack(Items.COAL), it("lignite"), it("coal_coke"));
        Slot wood = Slot.of(new ItemStack(Items.OAK_LOG), new ItemStack(Items.ACACIA_LOG), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_SAPLING));
        // ModItems.scrap = ModMaterialItems SCRAP/SCRAP (ID "scrap").
        Slot misc = Slot.of(it("solid_fuel"), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP, MaterialShape.SCRAP)), it("dust"), it("rocket_fuel"));
        Fluid[] smokes = {ModFluids.SMOKE.getSource(), ModFluids.SMOKE_LEADED.getSource(), ModFluids.SMOKE_POISON.getSource()};

        List<Entry> list = new ArrayList<>();
        list.add(new Entry(clean(ovens, coals), clean(Slot.of(it("ash_coal")))));
        list.add(new Entry(clean(ovens, wood), clean(Slot.of(it("ash_wood")))));
        list.add(new Entry(clean(ovens, misc), clean(Slot.of(it("ash_misc")))));
        for (Fluid smoke : smokes) {
            list.add(new Entry(clean(chimneys, Slot.fluid(smoke, 2_000)), clean(Slot.of(it("ash_fly")))));
            list.add(new Entry(clean(Slot.of(it("chimney_industrial")), Slot.fluid(smoke, 8_000)), clean(Slot.of(it("ash_soot")))));
        }
        list.removeIf(e -> e.outputs().isEmpty());
        return list;
    }

    // ---- DeuteriumHandler ----

    public static List<Entry> deuterium() {
        return List.of(new Entry(List.of(Slot.fluid(ModFluids.WATER.getSource(), 1_000)),
                List.of(Slot.fluid(ModFluids.HEAVYWATER.getSource(), 20))));
    }

    // ---- FusionBreederHandler / FluidBreederRecipes.getRecipes ----

    public static List<Entry> fusionBreeder() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Fluid, FluidBreederRecipes.FluidBreederRecipe> e : FluidBreederRecipes.getRecipes().entrySet()) {
            list.add(new Entry(List.of(Slot.fluid(e.getKey(), e.getValue().amountIn())),
                    List.of(Slot.fluid(e.getValue().output(), e.getValue().amountOut()))));
        }
        return list;
    }

    // ---- GrenadeRecipeHandler ----

    public static List<Entry> grenades() {
        List<Entry> list = new ArrayList<>();
        for (EnumGrenadeShell shell : EnumGrenadeShell.values()) for (EnumGrenadeFilling filling : EnumGrenadeFilling.values()) {
            if (filling.compatibleShells.contains(shell)) for (EnumGrenadeFuze fuze : EnumGrenadeFuze.values()) {
                list.add(grenade(shell, filling, fuze, null));
                for (EnumGrenadeExtra extra : EnumGrenadeExtra.values()) list.add(grenade(shell, filling, fuze, extra));
            }
        }
        return list;
    }

    private static Entry grenade(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, EnumGrenadeExtra extra) {
        List<Slot> ins = new ArrayList<>();
        ins.add(Slot.of(new ItemStack(GrenadeItems.shell(shell))));
        ins.add(Slot.of(new ItemStack(GrenadeItems.filling(filling))));
        ins.add(Slot.of(new ItemStack(GrenadeItems.fuze(fuze))));
        if (extra != null) ins.add(Slot.of(new ItemStack(GrenadeItems.extra(extra))));
        return new Entry(ins, List.of(Slot.of(ItemGrenadeUniversal.make(shell, filling, fuze, extra))));
    }
}
//?} else {
/*public final class NeiUniversalJeiRecipes {
    private NeiUniversalJeiRecipes() {}
}*///?}
