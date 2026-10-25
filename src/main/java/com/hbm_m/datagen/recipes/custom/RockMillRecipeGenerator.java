package com.hbm_m.datagen.recipes.custom;
//? if forge {

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code RockMillRecipes}: Zerkleinern von Gestein mit Wasser (je 25 HE/t), dabei faellt Kolloid oder Lava an;
 * aus jeder Charge kommt genau ein Ergebnis nach Gewicht. Dazu Ton aus Sand + Kolloid und Sand aus Kolloid.
 */
public final class RockMillRecipeGenerator {

    private static final int CONSUMPTION = 25;
    private static final int SHORT = 100;
    private static final int LONG = 200;

    private RockMillRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        Ingredient cobble = Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "cobblestone")));
        Ingredient sand = Ingredient.of(ItemTags.SAND);

        crush(writer, "cobble", cobble, Items.COBBLESTONE, SHORT, ModFluids.COLLOID, 250,
                new ItemStack(Items.GRAVEL), 95, stack("quartz_powder"), 5);
        crush(writer, "gravel", Ingredient.of(Items.GRAVEL), Items.GRAVEL, SHORT, ModFluids.COLLOID, 250,
                new ItemStack(Items.SAND), 75, new ItemStack(Items.FLINT), 20, stack("boron_powder"), 5);
        crush(writer, "sand", sand, Items.SAND, SHORT, ModFluids.COLLOID, 250,
                stack("dust"), 90, stack("calcium_powder"), 5, stack("fluorite"), 5);
        crush(writer, "netherrack", Ingredient.of(Items.NETHERRACK), Items.NETHERRACK, SHORT, ModFluids.LAVA, 100,
                new ItemStack(Items.GRAVEL), 50, new ItemStack(Items.SOUL_SAND), 25, new ItemStack(Items.GLOWSTONE_DUST), 15, stack("quartz_powder"), 10);
        crush(writer, "soulsand", Ingredient.of(Items.SOUL_SAND), Items.SOUL_SAND, SHORT, ModFluids.LAVA, 100,
                new ItemStack(Items.SAND), 50, stack("fire_powder"), 25, stack("uranium_powder"), 15, new ItemStack(Items.BLAZE_POWDER), 5, new ItemStack(Items.NETHER_WART), 5);
        crush(writer, "schist", Ingredient.of(item("stone_gneiss")), item("stone_gneiss"), LONG, ModFluids.COLLOID, 250,
                new ItemStack(Items.GRAVEL), 50, new ItemStack(Items.SAND), 10, stack("lithium_powder"), 25, stack("niobium_powder"), 5, stack("uranium_powder"), 5, stack("gold_powder"), 5);
        crush(writer, "basalt", Ingredient.of(item("basalt")), item("basalt"), LONG, ModFluids.COLLOID, 250,
                new ItemStack(Items.GRAVEL), 50, stack("ash_misc"), 25, stack("quartz_powder"), 15, stack("gravel_obsidian"), 10);
        crush(writer, "hematite", Ingredient.of(item("stone_resource_hematite")), item("stone_resource_hematite"), LONG, ModFluids.COLLOID, 250,
                new ItemStack(Items.GRAVEL), 65, stack("iron_powder"), 25, stack("titanium_powder"), 10);
        crush(writer, "bauxite", Ingredient.of(item("stone_resource_bauxite")), item("stone_resource_bauxite"), LONG, ModFluids.COLLOID, 250,
                new ItemStack(Items.GRAVEL), 25, new ItemStack(Items.CLAY_BALL), 25, stack("stone_resource_hematite"), 25, stack("titanium_ore"), 25);

        RockMillRecipeBuilder.rockMillRecipe(LONG, CONSUMPTION)
                .addItemInput(sand, 2)
                .addFluidInput(ModFluids.COLLOID.getSource(), 2_500)
                .addItemOutput(new ItemStack(Items.CLAY_BALL, 4))
                .save(writer, "rockmill/clay");
        RockMillRecipeBuilder.rockMillRecipe(SHORT, CONSUMPTION)
                .addFluidInput(ModFluids.COLLOID.getSource(), 1_000)
                .addItemOutput(new ItemStack(Items.SAND))
                .save(writer, "rockmill/colloid");
    }

    /** Zerkleinern: 1x Gestein + 250 mB Wasser -> Nebenprodukt-Fluessigkeit + eine Zufallsausgabe. */
    private static void crush(Consumer<FinishedRecipe> writer, String name, Ingredient input, Item icon, int duration,
                              ModFluids.FluidEntry outFluid, int outMb, Object... chances) {
        RockMillRecipeBuilder.rockMillRecipe(duration, CONSUMPTION)
                .addItemInput(input, 1)
                .addFluidInput(ModFluids.WATER.getSource(), 250)
                .addFluidOutput(outFluid.getSource(), outMb)
                .addChanceOutput(chances)
                .withIconItem(icon)
                .save(writer, "rockmill/" + name);
    }

    private static Item item(String id) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        if (it == Items.AIR) throw new IllegalStateException("Steinmuehle: unbekanntes Item hbm_m:" + id);
        return it;
    }

    private static ItemStack stack(String id) {
        return new ItemStack(item(id));
    }
}
//?}
