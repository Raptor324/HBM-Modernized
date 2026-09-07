package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code com.hbm.inventory.recipes.FusionRecipes#registerDefaults()} (1.7.10).
 *
 * <p>Alle zehn Fusionsrezepte inklusive Zuendschwelle, Plasmaleistung, Neutronenfluss und
 * Plasmafarbe. Der Neutronenfluss ist im Original als Bruchteil der Brueter-Kapazitaet
 * ({@code TileEntityFusionBreeder.capacity = 10_000}) angegeben - das ist hier ueber
 * {@link #BREEDER_CAPACITY} 1:1 nachgebildet.</p>
 */
public final class FusionRecipeGenerator {

    private FusionRecipeGenerator() {}

    /** Original: {@code TileEntityFusionBreeder.capacity}. */
    private static final double BREEDER_CAPACITY = 10_000D;
    /** Original: lokale Variable {@code solenoid} in {@code registerDefaults()}. */
    private static final long SOLENOID = 25_000L;
    /** Original: {@code .setDuration(100)} bei allen Rezepten. */
    private static final int DURATION = 100;

    public static void generate(Consumer<FinishedRecipe> writer) {

        // Deuterium-Deuterium: brueten von Helium und Tritium, Energie reicht zum Zuenden von TH4.
        // 15MHE/s bis 20MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(750_000L).outputEnergy(1_000_000L).outputFlux(BREEDER_CAPACITY / 200)
                .rgb(1F, 0.2F, 0.2F)
                .icon(ModFluids.DEUTERIUM.getSource())
                .addFluidInput(ModFluids.DEUTERIUM.getSource(), 20)
                .addFluidOutput(ModFluids.HELIUM4.getSource(), 1_000)
                .save(writer, "fusion/dd");

        // Frueher Brennstoff. 5MHE/s bis 20MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(250_000L).outputEnergy(1_250_000L).outputFlux(BREEDER_CAPACITY / 200)
                .icon(ModFluids.OXYGEN.getSource())
                .addFluidInput(ModFluids.DEUTERIUM.getSource(), 10)
                .addFluidInput(ModFluids.OXYGEN.getSource(), 10)
                .addItemOutput(new ItemStack(ModItems.PELLET_CHARGED.get()))
                .save(writer, "fusion/do");

        // Mittlerer Brennstoff. 15MHE/s bis 75MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(750_000L).outputEnergy(3_750_000L).outputFlux(BREEDER_CAPACITY / 100)
                .icon(ModFluids.HELIUM4.getSource())
                .addFluidInput(ModFluids.DEUTERIUM.getSource(), 10)
                .addFluidInput(ModFluids.TRITIUM.getSource(), 10)
                .addFluidOutput(ModFluids.HELIUM4.getSource(), 1_000)
                .save(writer, "fusion/dt");

        // Mittlerer Brennstoff, braucht drei Klystrons oder Tandembetrieb. 50MHE/s bis 125MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(2_500_000L).outputEnergy(6_250_000L).outputFlux(BREEDER_CAPACITY / 20)
                .rgb(0.8F, 0.6F, 0.4F)
                .icon(new ItemStack(ModItems.CHLOROPHYTE_POWDER.get()))
                .addFluidInput(ModFluids.TRITIUM.getSource(), 10)
                .addFluidInput(ModFluids.CHLORINE.getSource(), 10)
                .addItemOutput(new ItemStack(ModItems.CHLOROPHYTE_POWDER.get()))
                .save(writer, "fusion/tcl");

        // Mittlerer Brennstoff, aneutronisch. 10MHE/s bis 75MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(500_000L).outputEnergy(3_750_000L).outputFlux(0D)
                .rgb(0.2F, 0.2F, 1F)
                .icon(ModFluids.HELIUM3.getSource())
                .addFluidInput(ModFluids.HELIUM3.getSource(), 20)
                .addFluidOutput(ModFluids.HELIUM4.getSource(), 1_000)
                .save(writer, "fusion/h3");

        // Mittlerer Brennstoff, im Tandem mit DD. 17.5MHE/s bis 80MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(875_000L).outputEnergy(4_000_000L).outputFlux(BREEDER_CAPACITY / 20)
                .rgb(0.2F, 0.2F, 1F)
                .icon(ModFluids.TRITIUM.getSource())
                .addFluidInput(ModFluids.TRITIUM.getSource(), 10)
                .addFluidInput(ModFluids.HELIUM4.getSource(), 10)
                .addItemOutput(new ItemStack(ModItems.PELLET_CHARGED.get()))
                .save(writer, "fusion/th4");

        // Hoher Brennstoff, Zuendschwelle uebersteigt die Klystronleistung, braucht TH4 oder H3.
        // 75MHE/s bis 200MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(3_750_000L).outputEnergy(10_000_000L).outputFlux(BREEDER_CAPACITY / 10)
                .rgb(1F, 0.6F, 0.2F)
                .icon(new ItemStack(ModItems.CHLOROPHYTE_POWDER.get()))
                .addFluidInput(ModFluids.CHLORINE.getSource(), 20)
                .addItemOutput(new ItemStack(ModItems.CHLOROPHYTE_POWDER.get()))
                .save(writer, "fusion/cl");

        // Hoher Brennstoff, braucht die Chlorphase zum Zuenden. 200MHE/s bis 500MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(10_000_000L).outputEnergy(25_000_000L).outputFlux(BREEDER_CAPACITY / 5)
                .rgb(0.2F, 0.8F, 0.8F)
                .icon(ModFluids.DHC.getSource())
                .addFluidInput(ModFluids.DHC.getSource(), 20)
                .addItemOutput(new ItemStack(ModItems.CHLOROPHYTE_POWDER.get()))
                .save(writer, "fusion/dhc");

        // Hoher Brennstoff, niedrige Zuendschwelle. 20MHE/s bis 250MHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(1_000_000L).outputEnergy(12_500_000L).outputFlux(BREEDER_CAPACITY / 5)
                .rgb(0.2F, 1F, 0.2F)
                .icon(ModFluids.BALEFIRE.getSource())
                .addFluidInput(ModFluids.BALEFIRE.getSource(), 15)
                .addFluidInput(ModFluids.AMAT.getSource(), 5)
                .addItemOutput(new ItemStack(ModItems.POWDER_BALEFIRE.get()))
                .save(writer, "fusion/bf");

        // Hoher Brennstoff, hohe Zuendschwelle. 200MHE/s bis 1GHE/s
        FusionRecipeBuilder.fusionRecipe(DURATION, SOLENOID)
                .inputEnergy(10_000_000L).outputEnergy(50_000_000L).outputFlux(BREEDER_CAPACITY / 1)
                .rgb(1F, 0.4F, 0.1F)
                .icon(ModFluids.STELLAR_FLUX.getSource())
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 10)
                .addItemOutput(new ItemStack(ModItems.getPowders(ModPowders.GOLD).get()))
                .save(writer, "fusion/stellar");
    }
}
//?}
