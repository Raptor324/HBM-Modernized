package com.hbm_m.datagen;

import com.hbm_m.block.AnvilTier;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public final class AnvilRecipeGenerator {
    private AnvilRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerTieredRecipes(writer);
    }

    private static void registerTieredRecipes(Consumer<FinishedRecipe> writer) {
        registerCombineRecipes(writer);
        registerCraftRecipes(writer);
        registerDisassemblyRecipes(writer);
    }

    private static void registerCombineRecipes(Consumer<FinishedRecipe> writer) {
        registerCombineRecipe(writer, "iron", "metal_rod",
                stack(ModItems.PLATE_IRON, 2),
                stack(ModItems.INSULATOR),
                stack(ModItems.METAL_ROD, 2),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "steel", "combat_axe",
                stack(ModItems.PLATE_STEEL, 2),
                stack(ModItems.PLATE_IRON),
                stack(ModItems.STEEL_AXE),
                AnvilTier.STEEL);

        registerCombineRecipe(writer, "oil", "detector",
                stack(ModItems.PLATE_COPPER, 2),
                stack(ModItems.PLATE_ALUMINUM),
                stack(ModItems.OIL_DETECTOR),
                AnvilTier.OIL);

        registerCombineRecipe(writer, "nuclear", "radaway",
                stack(ModItems.PLATE_LEAD, 2),
                stack(ModItems.SULFUR, 3),
                stack(ModItems.RADAWAY, 2),
                AnvilTier.NUCLEAR);

        registerCombineRecipe(writer, "rbmk", "battery_advanced",
                stack(ModItems.BATTERY),
                stack(ModItems.PLATE_TITANIUM, 2),
                stack(ModItems.BATTERY_ADVANCED),
                AnvilTier.RBMK);

        registerCombineRecipe(writer, "fusion", "quantum_chip",
                stack(ModItems.SILICON_CIRCUIT),
                stack(ModItems.BISMOID_CIRCUIT),
                stack(ModItems.QUANTUM_CHIP),
                AnvilTier.FUSION);

        registerCombineRecipe(writer, "particle", "battery_spark",
                stack(ModItems.BATTERY_SCHRABIDIUM),
                stack(ModItems.BATTERY_LITHIUM_CELL),
                stack(ModItems.BATTERY_SPARK),
                AnvilTier.PARTICLE);

        registerCombineRecipe(writer, "gerald", "quantum_computer",
                stack(ModItems.CONTROLLER_ADVANCED),
                stack(ModItems.QUANTUM_CHIP),
                stack(ModItems.QUANTUM_COMPUTER),
                AnvilTier.GERALD);

        registerCombineRecipe(writer, "murky", "spark_cell_power",
                stack(ModItems.BATTERY_SPARK_CELL_1000),
                stack(ModItems.QUANTUM_COMPUTER),
                stack(ModItems.BATTERY_SPARK_CELL_POWER),
                AnvilTier.MURKY);
    }

    private static void registerCraftRecipes(Consumer<FinishedRecipe> writer) {
        registerInventoryRecipe(writer, "iron", "coil_copper",
                AnvilTier.IRON,
                stack(ModItems.COIL_COPPER),
                stack(ModItems.WIRE_COPPER, 4),
                stack(ModItems.PLATE_IRON),
                stack(ModItems.INSULATOR));

        registerInventoryRecipe(writer, "steel", "motor",
                AnvilTier.STEEL,
                stack(ModItems.MOTOR),
                stack(ModItems.PLATE_STEEL, 2),
                stack(ModItems.METAL_ROD, 2),
                stack(ModItems.COIL_COPPER));

        registerInventoryRecipe(writer, "oil", "depth_scanner",
                AnvilTier.OIL,
                stack(ModItems.DEPTH_ORES_SCANNER),
                stack(ModItems.OIL_DETECTOR),
                stack(ModItems.COIL_COPPER),
                stack(ModItems.PLATE_ALUMINUM));

        registerInventoryRecipe(writer, "nuclear", "battery_standard",
                AnvilTier.NUCLEAR,
                stack(ModItems.BATTERY),
                stack(ModItems.PLATE_LEAD, 2),
                stack(ModItems.WIRE_COPPER, 2),
                stack(ModItems.CAPACITOR));

        registerInventoryRecipe(writer, "rbmk", "motor_bismuth",
                AnvilTier.RBMK,
                stack(ModItems.MOTOR_BISMUTH),
                stack(ModItems.MOTOR),
                stack(ModItems.PLATE_BISMUTH),
                stack(ModItems.COIL_GOLD));

        registerInventoryRecipe(writer, "fusion", "controller_advanced",
                AnvilTier.FUSION,
                stack(ModItems.CONTROLLER_ADVANCED),
                stack(ModItems.CONTROLLER),
                stack(ModItems.QUANTUM_CHIP),
                stack(ModItems.CAPACITOR_BOARD));

        registerInventoryRecipe(writer, "particle", "battle_module",
                AnvilTier.PARTICLE,
                stack(ModItems.BATTLE_MODULE),
                stack(ModItems.BATTLE_SENSOR),
                stack(ModItems.BATTLE_GEARS),
                stack(ModItems.PLATE_SCHRABIDIUM));

        registerInventoryRecipe(writer, "gerald", "battle_counter",
                AnvilTier.GERALD,
                stack(ModItems.BATTLE_COUNTER),
                stack(ModItems.BATTLE_CASING),
                stack(ModItems.QUANTUM_COMPUTER),
                stack(ModItems.BATTERY_SPARK));

        registerInventoryRecipe(writer, "murky", "battle_casing",
                AnvilTier.MURKY,
                stack(ModItems.BATTLE_CASING),
                stack(ModItems.BATTLE_MODULE),
                stack(ModItems.PLATE_ARMOR_DNT),
                stack(ModItems.PLATE_ARMOR_HEV));
    }

    private static void registerDisassemblyRecipes(Consumer<FinishedRecipe> writer) {
        registerDisassemblyRecipe(writer, "iron", "coil_copper",
                AnvilTier.IRON,
                stack(ModItems.COIL_COPPER),
                stack(ModItems.WIRE_COPPER, 2),
                builder -> builder.addOutput(stack(ModItems.INSULATOR), 0.5F)
                        .addOutput(stack(ModItems.METAL_ROD), 0.35F));

        registerDisassemblyRecipe(writer, "steel", "steel_weapon",
                AnvilTier.STEEL,
                stack(ModItems.STEEL_AXE),
                stack(ModItems.PLATE_STEEL, 2),
                builder -> builder.addOutput(stack(ModItems.PLATE_IRON), 0.6F)
                        .addOutput(stack(ModItems.BOLT_STEEL), 0.35F));

        registerDisassemblyRecipe(writer, "oil", "oil_detector",
                AnvilTier.OIL,
                stack(ModItems.OIL_DETECTOR),
                stack(ModItems.PLATE_COPPER),
                builder -> builder.addOutput(stack(ModItems.PLATE_ALUMINUM), 1.0F)
                        .addOutput(stack(ModItems.INSULATOR), 0.5F));

        registerDisassemblyRecipe(writer, "nuclear", "battery_breakdown",
                AnvilTier.NUCLEAR,
                stack(ModItems.BATTERY),
                stack(ModItems.WIRE_COPPER, 2),
                builder -> builder.addOutput(stack(ModItems.PLATE_LEAD), 1.0F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.SULFUR), 0.5F));

        registerDisassemblyRecipe(writer, "rbmk", "battery_advanced",
                AnvilTier.RBMK,
                stack(ModItems.BATTERY_ADVANCED),
                stack(ModItems.BATTERY_LITHIUM_CELL),
                builder -> builder.addOutput(stack(ModItems.PLATE_TITANIUM, 2), 1.0F)
                        .addOutput(stack(ModItems.COIL_GOLD), 0.45F));

        registerDisassemblyRecipe(writer, "fusion", "quantum_chip",
                AnvilTier.FUSION,
                stack(ModItems.QUANTUM_CHIP),
                stack(ModItems.BISMOID_CIRCUIT),
                builder -> builder.addOutput(stack(ModItems.SILICON_CIRCUIT), 1.0F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.5F));

        registerDisassemblyRecipe(writer, "particle", "battery_spark",
                AnvilTier.PARTICLE,
                stack(ModItems.BATTERY_SPARK),
                stack(ModItems.BATTERY_SCHRABIDIUM),
                builder -> builder.addOutput(stack(ModItems.BATTERY_LITHIUM_CELL), 1.0F)
                        .addOutput(stack(ModItems.QUANTUM_CHIP), 0.3F));

        registerDisassemblyRecipe(writer, "gerald", "quantum_computer",
                AnvilTier.GERALD,
                stack(ModItems.QUANTUM_COMPUTER),
                stack(ModItems.CONTROLLER_ADVANCED),
                builder -> builder.addOutput(stack(ModItems.QUANTUM_CHIP), 1.0F)
                        .addOutput(stack(ModItems.BATTERY_SPARK), 0.5F));

        registerDisassemblyRecipe(writer, "murky", "spark_cell_power",
                AnvilTier.MURKY,
                stack(ModItems.BATTERY_SPARK_CELL_POWER),
                stack(ModItems.BATTERY_SPARK_CELL_1000),
                builder -> builder.addOutput(stack(ModItems.BATTERY_SPARK), 1.0F)
                        .addOutput(stack(ModItems.BATTLE_MODULE), 0.35F));
    }

    private static void registerCombineRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                              ItemStack inputA, ItemStack inputB, ItemStack output,
                                              AnvilTier tier) {
        AnvilRecipeBuilder.anvilRecipe(inputA, inputB, output, tier)
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .save(writer, anvilId(tierFolder, "combine", name));
    }

    private static void registerInventoryRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                AnvilTier tier, ItemStack output, ItemStack... requirements) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.anvilRecipe(ItemStack.EMPTY, ItemStack.EMPTY, output, tier)
                .withOverlay(AnvilRecipe.OverlayType.CONSTRUCTION);
        for (ItemStack stack : requirements) {
            builder.addInventoryRequirement(stack);
        }
        builder.save(writer, anvilId(tierFolder, "craft", name));
    }

    private static void registerDisassemblyRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                  AnvilTier tier, ItemStack dismantled, ItemStack primaryOutput,
                                                  Consumer<AnvilRecipeBuilder> outputs) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.anvilRecipe(ItemStack.EMPTY, ItemStack.EMPTY, primaryOutput, tier)
                .withOverlay(AnvilRecipe.OverlayType.RECYCLING)
                .addInventoryRequirement(dismantled)
                .clearOutputs()
                .addOutput(primaryOutput, 1.0F);
        outputs.accept(builder);
        builder.save(writer, anvilId(tierFolder, "disassemble", name));
    }

    private static ResourceLocation anvilId(String tierFolder, String category, String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                "anvil/" + tierFolder + "/" + category + "_" + name);
    }

    private static ItemStack stack(RegistryObject<Item> item) {
        return stack(item, 1);
    }

    private static ItemStack stack(RegistryObject<Item> item, int count) {
        return new ItemStack(item.get(), count);
    }

}

