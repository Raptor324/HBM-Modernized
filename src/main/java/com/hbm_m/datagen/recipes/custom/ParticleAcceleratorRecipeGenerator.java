package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.item.ModItems;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Port von {@code ParticleAcceleratorRecipes.register()} (1.7.10).
 *
 * <p>Elf der zwoelf Rezepte des Originals. Das fehlende ({@code item_expensive} Golddust plus
 * Schrabidium-Barren zu entartetem Material) haengt am {@code item_expensive}-System, das dieser
 * Port nicht hat.</p>
 */
public final class ParticleAcceleratorRecipeGenerator {

    private ParticleAcceleratorRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        pa(writer, "amat",       ModItems.PARTICLE_HYDROGEN.get(),   ModItems.PARTICLE_COPPER.get(),      300,    ModItems.PARTICLE_AMAT.get());
        pa(writer, "aschrab",    ModItems.PARTICLE_AMAT.get(),       ModItems.PARTICLE_AMAT.get(),        400,    ModItems.PARTICLE_ASCHRAB.get());
        pa(writer, "dark",       ModItems.PARTICLE_ASCHRAB.get(),    ModItems.PARTICLE_ASCHRAB.get(),     10_000, ModItems.PARTICLE_DARK.get());
        pa(writer, "muon",       ModItems.PARTICLE_HYDROGEN.get(),   ModItems.PARTICLE_AMAT.get(),        2_500,  ModItems.PARTICLE_MUON.get());
        pa(writer, "higgs",      ModItems.PARTICLE_HYDROGEN.get(),   ModItems.PARTICLE_LEAD.get(),        6_500,  ModItems.PARTICLE_HIGGS.get());
        pa(writer, "tachyon",    ModItems.PARTICLE_MUON.get(),       ModItems.PARTICLE_HIGGS.get(),       5_000,  ModItems.PARTICLE_TACHYON.get());
        pa(writer, "strange",    ModItems.PARTICLE_MUON.get(),       ModItems.PARTICLE_DARK.get(),        12_500, ModItems.PARTICLE_STRANGE.get());
        pa(writer, "digamma",    ModItems.PARTICLE_SPARKTICLE.get(), ModItems.PARTICLE_HIGGS.get(),       70_000, ModItems.PARTICLE_DIGAMMA.get());

        // Original: liefert zusaetzlich Staub.
        ParticleAcceleratorRecipeBuilder
                .paRecipe(Ingredient.of(ModItems.PARTICLE_STRANGE.get()),
                          Ingredient.of(ModItems.POWDER_MAGIC.get()),
                          12_500,
                          new ItemStack(ModItems.PARTICLE_SPARKTICLE.get()))
                .secondResult(new ItemStack(ModItems.DUST.get()))
                .save(writer, "particle_accelerator/sparkticle");

        // Original: das Huhn-Rezept, zwei Nuggets als Ausbeute.
        ParticleAcceleratorRecipeBuilder
                .paRecipe(Ingredient.of(Items.CHICKEN), Ingredient.of(Items.CHICKEN), 100,
                          new ItemStack(ModItems.NUGGET.get()))
                .secondResult(new ItemStack(ModItems.NUGGET.get()))
                .save(writer, "particle_accelerator/chicken");
    }

    private static void pa(Consumer<FinishedRecipe> writer, String name,
                           ItemLike inputA, ItemLike inputB, int momentum, ItemLike output) {
        ParticleAcceleratorRecipeBuilder
                .paRecipe(Ingredient.of(inputA), Ingredient.of(inputB), momentum, new ItemStack(output))
                .save(writer, "particle_accelerator/" + name);
    }
}
//?}
