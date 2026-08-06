package com.hbm_m.datagen.recipes;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

// Провайдер генерации рецептов крафта для мода.
// Здесь мы определяем, как создаются наши предметы в игре.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.datagen.recipes.custom.AmmoPressRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.AnvilRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.PurexRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.AssemblerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.BlastFurnaceRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CentrifugeRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ChemicalPlantRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.PressRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ShredderRecipeGenerator;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;

public class ModRecipeProvider extends RecipeProvider {

    private final PackOutput packOutput;

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
        this.packOutput = pOutput;
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> pWriter) {

        BlastFurnaceRecipeGenerator.generate(pWriter);
        PressRecipeGenerator.generate(pWriter);
        AssemblerRecipeGenerator.generate(pWriter);
        ChemicalPlantRecipeGenerator.generate(pWriter);
        AnvilRecipeGenerator.generate(pWriter);
        ShredderRecipeGenerator.generate(pWriter, ModRecipeProvider::unlockedByItem);
        CentrifugeRecipeGenerator.generate(pWriter);
        AmmoPressRecipeGenerator.generate(pWriter);
        PurexRecipeGenerator.generate(pWriter);

        // ==================== АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ РЕЦЕПТОВ ДЛЯ БЛОКОВ СЛИТКОВ ====================
        for (ModIngots ingot : ModIngots.values()) {

            // !!! ВАЖНОЕ ИСПРАВЛЕНИЕ !!!
            // Сначала проверяем, есть ли вообще блок у этого слитка.
            // Если блока нет (например, у gunsteel или mud), мы пропускаем этот шаг, чтобы избежать краша.
            if (!ModBlocks.hasIngotBlock(ingot)) {
                continue;
            }

            // Теперь безопасно получаем предмет и блок
            var ingotItem = ModItems.getIngot(ingot);
            var ingotBlock = ModBlocks.getIngotBlock(ingot);

            if (ingotItem != null && ingotBlock != null) {
                String ingotName = ingot.getName();

                // Рецепт: 9 слитков -> 1 блок (Shaped Recipe 3x3)
                ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.MISC, ingotBlock.get())
                        .pattern("III")
                        .pattern("III")
                        .pattern("III")
                        .define('I', ingotItem.get())
                        .unlockedBy("has_" + ingotName + "_ingot", has(ingotItem.get()))
                        .save(pWriter, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                com.hbm_m.lib.RefStrings.MODID, ingotName + "_block_from_ingots"));

                // Рецепт: 1 блок -> 9 слитков (Shapeless Recipe)
                ShapelessRecipeBuilder.shapeless(net.minecraft.data.recipes.RecipeCategory.MISC, ingotItem.get(), 9)
                        .requires(ingotBlock.get())
                        .unlockedBy("has_" + ingotName + "_block", has(ingotBlock.get()))
                        .save(pWriter, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                com.hbm_m.lib.RefStrings.MODID, ingotName + "_ingots_from_block"));
            }
        }

        // Fallout (1.7.10 MineralRecipes: block_fallout ↔ fallout, ковёр из 2 pile)
        ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.MISC, ModBlocks.BLOCK_FALLOUT.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.FALLOUT.get())
                .unlockedBy("has_fallout", has(ModItems.FALLOUT.get()))
                .save(pWriter, "block_fallout_from_fallout");

        ShapelessRecipeBuilder.shapeless(net.minecraft.data.recipes.RecipeCategory.MISC, ModItems.FALLOUT.get(), 9)
                .requires(ModBlocks.BLOCK_FALLOUT.get())
                .unlockedBy("has_block_fallout", has(ModBlocks.BLOCK_FALLOUT.get()))
                .save(pWriter, "fallout_from_block_fallout");

        ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.MISC, ModBlocks.NUCLEAR_FALLOUT.get(), 2)
                .pattern("##")
                .define('#', ModItems.FALLOUT.get())
                .unlockedBy("has_fallout", has(ModItems.FALLOUT.get()))
                .save(pWriter, "nuclear_fallout_from_fallout");

        // Delegate vanilla-style recipes so they share a single RecipeProvider registration.
        new ModVanillaRecipeProvider(this.packOutput).registerVanillaRecipes(pWriter);
    }

    protected static InventoryChangeTrigger.TriggerInstance unlockedByItem(ItemLike itemLike) {
        return has(itemLike);
    }
}