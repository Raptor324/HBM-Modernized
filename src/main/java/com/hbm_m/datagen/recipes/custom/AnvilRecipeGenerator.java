package com.hbm_m.datagen.recipes.custom;

import com.hbm_m.block.custom.machines.anvils.AnvilTier;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
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

        registerCombineRecipe(writer, "iron", "silicon",
                stack(ModItems.CINNABAR.get(), 1),
                stack(ModItems.BORAX.get(), 1),
                stack(ModItems.BILLET_SILICON.get(), 3),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_steel",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.STEEL).get(), 10),
                stack(ModBlocks.ANVIL_STEEL.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_desh",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.DESH).get(), 10),
                stack(ModBlocks.ANVIL_DESH.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_ferrouranium",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.FERROURANIUM).get(), 10),
                stack(ModBlocks.ANVIL_FERROURANIUM.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_saturnite",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.SATURNITE).get(), 10),
                stack(ModBlocks.ANVIL_SATURNITE.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_bismuth_bronze",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.BISMUTH_BRONZE).get(), 10),
                stack(ModBlocks.ANVIL_BISMUTH_BRONZE.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_arsenic_bronze",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.ARSENIC_BRONZE).get(), 10),
                stack(ModBlocks.ANVIL_ARSENIC_BRONZE.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_schrabidate",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.SCHRABIDATE).get(), 10),
                stack(ModBlocks.ANVIL_SCHRABIDATE.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_dineutronium",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.DINEUTRONIUM).get(), 10),
                stack(ModBlocks.ANVIL_DNT.get(), 1),
                AnvilTier.IRON);

        registerCombineRecipe(writer, "iron", "anvil_osmiridium",
                stack(ModBlocks.ANVIL_IRON.get(), 1),
                stack(ModItems.getIngot(ModIngots.OSMIRIDIUM).get(), 10),
                stack(ModBlocks.ANVIL_OSMIRIDIUM.get(), 1),
                AnvilTier.IRON);



    }

    private static void registerCraftRecipes(Consumer<FinishedRecipe> writer) {

        registerInventoryRecipe(writer, "steel", "1coil_copper_torus",
                AnvilTier.STEEL,
                stack(ModItems.COIL_COPPER_TORUS),
                stack(ModItems.COIL_COPPER, 2 ));

        registerInventoryRecipe(writer, "steel", "2coil_gold_torus",
                AnvilTier.STEEL,
                stack(ModItems.COIL_GOLD_TORUS),
                stack(ModItems.COIL_GOLD, 2 ));

        registerInventoryRecipe(writer, "steel", "3coil_alloy_torus",
                AnvilTier.STEEL,
                stack(ModItems.COIL_ADVANCED_ALLOY_TORUS),
                stack(ModItems.COIL_ADVANCED_ALLOY, 2 ));

        registerInventoryRecipe(writer, "steel", "4coil_tungsten_torus",
                AnvilTier.STEEL,
                stack(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS),
                stack(ModItems.COIL_MAGNETIZED_TUNGSTEN, 2 ));

        registerInventoryRecipe(writer, "steel", "5silicon",
                AnvilTier.STEEL,
                stack(ModItems.SILICON_CIRCUIT),
                stack(ModItems.BILLET_SILICON, 1 ));

        registerInventoryRecipe(writer, "steel", "6motor",
                AnvilTier.STEEL,
                stack(ModItems.MOTOR),
                stack(ModItems.COIL_COPPER, 1 ),
                stack(ModItems.PLATE_IRON, 2 ),
                stack(ModItems.COIL_COPPER_TORUS, 1 ));

        registerInventoryRecipe(writer, "iron", "7blast_furnace",
                AnvilTier.IRON,
                stack(ModBlocks.BLAST_FURNACE),
                stack(ModItems.PLATE_COPPER, 4),
                stack(Items.STONE_BRICKS, 4 ),
                stack(ModItems.FIREBRICK, 4 ));

        registerInventoryRecipe(writer, "steel", "8advanced_assemble_machine",
                AnvilTier.STEEL,
                stack(ModBlocks.ADVANCED_ASSEMBLY_MACHINE),
                stack(ModItems.PLATE_COPPER, 4),
                stack(ModItems.getIngot(ModIngots.STEEL).get(), 8),
                stack(ModItems.MOTOR, 2 ),
                stack(ModItems.VACUUM_TUBE, 4 ));

    }

    private static void registerDisassemblyRecipes(Consumer<FinishedRecipe> writer) {

        registerDisassemblyRecipe(writer, "steel", "crt_breakdown1",
                AnvilTier.STEEL,
                stack(ModBlocks.CRT_BROKEN),
                stack(ModItems.PLATE_STEEL, 4),
                builder -> builder.addOutput(stack(ModItems.PLATE_LEAD), 1.0F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.PCB), 1F)
                        .addOutput(stack(ModItems.ANALOG_CIRCUIT), 0.4F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.MICROCHIP), 0.5F));

        registerDisassemblyRecipe(writer, "steel", "crt_breakdown2",
                AnvilTier.STEEL,
                stack(ModBlocks.CRT_BSOD),
                stack(ModItems.PLATE_STEEL, 4),
                builder -> builder.addOutput(stack(ModItems.PLATE_LEAD), 1.0F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.PCB), 1F)
                        .addOutput(stack(ModItems.ANALOG_CIRCUIT), 0.4F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.MICROCHIP), 0.5F));

        registerDisassemblyRecipe(writer, "steel", "crt_breakdown3",
                AnvilTier.STEEL,
                stack(ModBlocks.CRT_CLEAN),
                stack(ModItems.PLATE_STEEL, 4),
                builder -> builder.addOutput(stack(ModItems.PLATE_LEAD), 1.0F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.PCB), 1F)
                        .addOutput(stack(ModItems.ANALOG_CIRCUIT), 0.4F)
                        .addOutput(stack(ModItems.CAPACITOR_BOARD), 0.4F)
                        .addOutput(stack(ModItems.MICROCHIP), 0.5F));

        registerDisassemblyRecipe(writer, "steel", "tape_breakdown",
                AnvilTier.STEEL,
                stack(ModBlocks.TAPE_RECORDER),
                stack(ModItems.PLATE_STEEL, 4),
                builder -> builder.addOutput(stack(ModItems.PLATE_LEAD), 1.0F)
                        .addOutput(stack(ModItems.WIRE_RED_COPPER), 0.5F)
                        .addOutput(stack(ModItems.VACUUM_TUBE), 0.5F)
                        .addOutput(stack(ModItems.MICROCHIP), 0.1F));

        registerDisassemblyRecipe(writer, "steel", "cabinet_breakdown",
                AnvilTier.STEEL,
                stack(ModBlocks.FILE_CABINET),
                stack(ModItems.PLATE_STEEL, 4),
                builder -> builder.addOutput(stack(ModItems.DUST), 1.0F));

        registerDisassemblyRecipe(writer, "steel", "toaster_breakdown",
                AnvilTier.STEEL,
                stack(ModBlocks.TOASTER),
                stack(ModItems.PLATE_STEEL, 2),
                builder -> builder
                        .addOutput(stack(ModItems.MAN_CORE), 0.001F));

        registerDisassemblyRecipe(writer, "steel", "freaky_breakdown",
                AnvilTier.STEEL,
                stack(ModBlocks.FREAKY_ALIEN_BLOCK),
                stack(ModItems.CANNED_JIZZ, 1),
                builder -> builder
                        .addOutput(stack(ModItems.CANNED_ASBESTOS), 0.5F));
    }

    /**
     * Регистрирует рецепт объединения (Combine Recipe).
     *
     * @param writer      Потребитель рецептов
     * @param tierFolder  Папка тира (iron, steel...)
     * @param name        Имя рецепта
     * @param inputA      Первый входной слот
     * @param inputB      Второй входной слот
     * @param output      Результат
     * @param tier        Минимальный тир наковальни
     */
    private static void registerCombineRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                ItemStack inputA, ItemStack inputB, ItemStack output,
                                                AnvilTier tier) {
        // Вызываем перегруженный метод без дополнительных настроек
        registerCombineRecipe(writer, tierFolder, name, inputA, inputB, output, tier, builder -> {});
    }

    /**
     * Регистрирует рецепт объединения с дополнительными настройками (например, сохранение предметов).
     */
    private static void registerCombineRecipe(Consumer<FinishedRecipe> writer, String tierFolder, String name,
                                                ItemStack inputA, ItemStack inputB, ItemStack output,
                                                AnvilTier tier, Consumer<AnvilRecipeBuilder> settings) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.anvilRecipe(inputA, inputB, output, tier)
                .withOverlay(AnvilRecipe.OverlayType.SMITHING);
        
        // Применяем пользовательские настройки (здесь можно вызвать .keepInputA() и т.д.)
        settings.accept(builder);

        builder.save(writer, anvilId(tierFolder, "combine", name));
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

    private static ItemStack stack(Object obj, int count) {
        if (obj instanceof RegistryObject<?>) {
            Object val = ((RegistryObject<?>) obj).get();
            if (val instanceof Item) {
                return new ItemStack((Item) val, count);
            } else if (val instanceof Block) {
                return new ItemStack(((Block) val).asItem(), count);
            }
        } else if (obj instanceof Item) {
            return new ItemStack((Item) obj, count);
        } else if (obj instanceof Block) {
            return new ItemStack(((Block) obj).asItem(), count);
        }
        throw new IllegalArgumentException("Unsupported object for stack: " + obj);
    }

    private static ItemStack stack(Object obj) {
        return stack(obj, 1);
    }


}

