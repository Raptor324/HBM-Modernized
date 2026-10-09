package com.hbm_m.datagen.recipes;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

// Провайдер генерации рецептов крафта для мода.
// Здесь мы определяем, как создаются наши предметы в игре.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.datagen.recipes.custom.AmmoPressRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.AnvilRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ArcWelderRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.PurexRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.AssemblerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.BlastFurnaceRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.BreederRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CatalyticReformerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ParticleAcceleratorRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CentrifugeRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ChemicalPlantRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CokerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CompressorRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CrackingTowerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CrystallizerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.CyclotronRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.FusionRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.PlasmaForgeRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ExposureChamberRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.FractionTowerRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.GasCentrifugeRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.HydrotreaterRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.LiquefactorRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.MachineCraftingRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.PressRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.PyroOvenRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.RadGenRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.RadiolysisRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.ShredderRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.SilexRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.SolidificationRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.SolderingRecipeGenerator;
import com.hbm_m.datagen.recipes.custom.VacuumDistillRecipeGenerator;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> rawWriter) {
        // Vom Original abweichende Port-Werkbankrezepte weglassen (ersetzt durch OrigCraftingRecipeGenerator)
        Consumer<FinishedRecipe> pWriter = com.hbm_m.datagen.recipes.custom.SupersededRecipes.filter(
                com.hbm_m.datagen.recipes.custom.OrigMachineRecipeGenerator.filter(rawWriter));
        com.hbm_m.datagen.recipes.custom.OrigCraftingRecipeGenerator.generate(pWriter);
        // Fehlende Schredder-/Kristallisierer-/Ofenrezepte des Originals (rc/machines.py) + Handuebersetzungen
        com.hbm_m.datagen.recipes.custom.OrigMachineRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.RestRecipeGenerator.generate(pWriter);
        // Konfig-Rezepte (LBSM-Hilfsaufrufe/Feindraht/Legierungsofen) - rc/config_extra.py
        com.hbm_m.datagen.recipes.custom.ConfigVariantRecipeGenerator.generate(pWriter);

        BlastFurnaceRecipeGenerator.generate(pWriter);
        PressRecipeGenerator.generate(pWriter);
        // 1:1 aus AssemblyMachineRecipes (automatisch uebersetzt) + verbliebene Port-Notbehelfe
        com.hbm_m.datagen.recipes.custom.AssemblyMachineRecipeGenerator.generate(pWriter);
        AssemblerRecipeGenerator.generate(pWriter);
        ChemicalPlantRecipeGenerator.generate(pWriter);
        AnvilRecipeGenerator.generate(pWriter);
        MachineCraftingRecipeGenerator.generate(pWriter);
        ShredderRecipeGenerator.generate(pWriter, ModRecipeProvider::unlockedByItem);
        CentrifugeRecipeGenerator.generate(pWriter);
        CrystallizerRecipeGenerator.generate(pWriter);
        CyclotronRecipeGenerator.generate(pWriter);
        FusionRecipeGenerator.generate(pWriter);
        PlasmaForgeRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.ConsumableRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.RodRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.WeaponRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.GrenadeRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.ArmorRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.CustomMachineRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.LegacyBedrockOreRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.ToolRecipeGenerator.generate(pWriter);
        // Mischer: Rezepte wie im Original statisch in com.hbm_m.recipe.MixerRecipes
        ArcWelderRecipeGenerator.generate(pWriter);
        SolderingRecipeGenerator.generate(pWriter);
        GasCentrifugeRecipeGenerator.generate(pWriter);
        AmmoPressRecipeGenerator.generate(pWriter);
        PurexRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.SuperComputerRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.RockMillRecipeGenerator.generate(pWriter);
        com.hbm_m.datagen.recipes.custom.PrecAssRecipeGenerator.generate(pWriter);
        BreederRecipeGenerator.generate(pWriter);
        RadGenRecipeGenerator.generate(pWriter);
        ExposureChamberRecipeGenerator.generate(pWriter);
        SilexRecipeGenerator.generate(pWriter);
        CokerRecipeGenerator.generate(pWriter);
        CompressorRecipeGenerator.generate(pWriter);
        CrackingTowerRecipeGenerator.generate(pWriter);
        FractionTowerRecipeGenerator.generate(pWriter);
        HydrotreaterRecipeGenerator.generate(pWriter);
        CatalyticReformerRecipeGenerator.generate(pWriter);
        ParticleAcceleratorRecipeGenerator.generate(pWriter);
        LiquefactorRecipeGenerator.generate(pWriter);
        PyroOvenRecipeGenerator.generate(pWriter);
        RadiolysisRecipeGenerator.generate(pWriter);
        SolidificationRecipeGenerator.generate(pWriter);
        VacuumDistillRecipeGenerator.generate(pWriter);

        // ==================== АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ РЕЦЕПТОВ ДЛЯ БЛОКОВ СЛИТКОВ ====================
        for (ModMaterials mat : ModMaterials.values()) {
            // Barren ueber ModMaterialItems.ingotMaterial (block_thorium <-> th232_ingot wie Original)
            if (!ModMaterialItems.has(ModMaterialItems.ingotMaterial(mat), MaterialShape.INGOT)) continue;

            // !!! ВАЖНОЕ ИСПРАВЛЕНИЕ !!!
            // Сначала проверяем, есть ли вообще блок у этого слитка.
            // Если блока нет (например, у gunsteel или mud), мы пропускаем этот шаг, чтобы избежать краша.
            if (!ModBlocks.hasIngotBlock(mat)) {
                continue;
            }

            // Теперь безопасно получаем предмет и блок
            var ingotItem = ModMaterialItems.get(ModMaterialItems.ingotMaterial(mat), MaterialShape.INGOT);
            var ingotBlock = ModBlocks.getIngotBlock(mat);

            if (ingotItem != null && ingotItem.isPresent() && ingotBlock != null && ingotBlock.isPresent()) {
            String ingotName = mat.getId();

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