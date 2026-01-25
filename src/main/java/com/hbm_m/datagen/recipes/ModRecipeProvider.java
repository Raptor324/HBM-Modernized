package com.hbm_m.datagen.recipes;

// Провайдер генерации рецептов крафта для мода.
// Здесь мы определяем, как создаются наши предметы в игре.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.datagen.recipes.custom.*;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

public class ModRecipeProvider extends RecipeProvider {

    private final PackOutput packOutput;

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
        this.packOutput = pOutput;
    }

    @Override
    protected void buildRecipes(@Nonnull Consumer<FinishedRecipe> pWriter) {

        BlastFurnaceRecipeGenerator.generate(pWriter);
        PressRecipeGenerator.generate(pWriter);
        AssemblerRecipeGenerator.generate(pWriter);
        AnvilRecipeGenerator.generate(pWriter);
        ShredderRecipeGenerator.generate(pWriter, ModRecipeProvider::unlockedByItem);

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
                        .save(pWriter, ingotName + "_block_from_ingots");

                // Рецепт: 1 блок -> 9 слитков (Shapeless Recipe)
                ShapelessRecipeBuilder.shapeless(net.minecraft.data.recipes.RecipeCategory.MISC, ingotItem.get(), 9)
                        .requires(ingotBlock.get())
                        .unlockedBy("has_" + ingotName + "_block", has(ingotBlock.get()))
                        .save(pWriter, ingotName + "_ingots_from_block");
            }
        }

        // Delegate vanilla-style recipes so they share a single RecipeProvider registration.
        new ModVanillaRecipeProvider(this.packOutput).registerVanillaRecipes(pWriter);
    }

    protected static InventoryChangeTrigger.TriggerInstance unlockedByItem(ItemLike itemLike) {
        return has(itemLike);
    }
}