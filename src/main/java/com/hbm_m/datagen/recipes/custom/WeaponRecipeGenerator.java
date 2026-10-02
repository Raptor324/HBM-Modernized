package com.hbm_m.datagen.recipes.custom;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.m;
import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;

import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Anfang des 1:1-Ports von {@code com.hbm.crafting.WeaponRecipes}. Bisher die Landminen; der Rest
 * (Waffen, Munition, mine_fat mit ammo_standard NUKE_DEMO) folgt mit dem Waffensystem.
 */
public final class WeaponRecipeGenerator {

    private WeaponRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> w) {
        CraftingGen g = new CraftingGen(w, "weapons");

        // ANY_SMOKELESS.dust() = ballistite, cordite; ANY_HIGHEXPLOSIVE.ingot() = ball_tnt, ball_tatb
        Ingredient anySmokeless = Ingredient.of(ModItems.BALLISTITE.get(), ModItems.CORDITE.get());
        Ingredient anyHighExplosive = Ingredient.of(ModItems.BALL_TNT.get(), ModItems.BALL_TATB.get());

        g.shaped(ModBlocks.MINE_AP.get(), 4, p("I", "C", "S"), 'I', m(ModMaterials.POLYMER, MaterialShape.PLATE), 'C', anySmokeless, 'S', m(ModMaterials.STEEL, MaterialShape.INGOT));
        g.shaped(ModBlocks.MINE_SHRAP.get(), 1, p("L", "M"), 'M', ModBlocks.MINE_AP.get(), 'L', ModItems.PELLET_BUCKSHOT);
        g.shaped(ModBlocks.MINE_HE.get(), 1, p(" C ", "PTP"), 'C', ModItems.INTEGRATED_CIRCUIT, 'P', m(ModMaterials.STEEL, MaterialShape.PLATE), 'T', anyHighExplosive);
    }
}
