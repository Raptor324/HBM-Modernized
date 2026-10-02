package com.hbm_m.inventory.fluid;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;

import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

/**
 * Die Welt-Fluessigkeiten des Originals ({@code ModBlocks.*_fluid} + {@code *_block}), die im Port
 * bisher fehlten bzw. nur als starre Platzhalterbloecke existierten. Die Werte (Dichte, Zaehigkeit,
 * Leuchtkraft, Temperatur) stammen 1:1 aus {@code ModBlocks.initializeBlock}; {@code quantaPerBlock
 * = 4} des Originals entspricht Vanillas {@code dropOff = 2} (drei Bloecke Ausbreitung),
 * die Tickrate der Bloecke der {@code tickDelay}.
 */
public final class WorldFluids {

    private WorldFluids() {}

    public static final class Entry {
        public final RegistrySupplier<Fluid> source;
        public final RegistrySupplier<Fluid> flowing;

        Entry(RegistrySupplier<Fluid> source, RegistrySupplier<Fluid> flowing) {
            this.source = source;
            this.flowing = flowing;
        }

        public FlowingFluid getSource() { return (FlowingFluid) source.get(); }
        public FlowingFluid getFlowing() { return (FlowingFluid) flowing.get(); }
    }

    private static Entry register(String name, String texture, int density, int viscosity, int luminosity, int temperature,
                                  int dropOff, int tickDelay, Supplier<RegistrySupplier<? extends LiquidBlock>> block,
                                  Supplier<RegistrySupplier<net.minecraft.world.item.Item>> bucket) {
        final RegistrySupplier<?>[] sourceRef = new RegistrySupplier[1];
        final RegistrySupplier<?>[] flowingRef = new RegistrySupplier[1];

        SimpleArchitecturyFluidAttributes attributes = SimpleArchitecturyFluidAttributes
                .ofSupplier(() -> (Supplier<? extends Fluid>) flowingRef[0], () -> (Supplier<? extends Fluid>) sourceRef[0])
                .sourceTexture(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/fluid/" + texture + "_still"))
                .flowingTexture(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/fluid/" + texture + "_flowing"))
                .density(density)
                .viscosity(viscosity)
                .luminosity(luminosity)
                .temperature(temperature)
                .dropOff(dropOff)
                .slopeFindDistance(4)
                .tickDelay(tickDelay)
                .explosionResistance(500F)
                .convertToSource(false)
                .blockSupplier(block)
                .fillSound(SoundEvents.BUCKET_FILL)
                .emptySound(SoundEvents.BUCKET_EMPTY);
        if (bucket != null) attributes.bucketItemSupplier(bucket);

        RegistrySupplier<Fluid> source = ModFluids.FLUIDS.register(
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, name), () -> new HbmFlowingFluid.Source(attributes));
        RegistrySupplier<Fluid> flowing = ModFluids.FLUIDS.register(
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, name + "_flowing"), () -> new HbmFlowingFluid.Flowing(attributes));
        sourceRef[0] = source;
        flowingRef[0] = flowing;
        return new Entry(source, flowing);
    }

    // mud_fluid: Density 2500, Viscosity 3000, Luminosity 5, Temperature 2773, tickRate 3000 / 200 = 15
    public static final Entry MUD = register("mud_fluid", "mud", 2500, 3000, 5, 2773, 2, 15,
            () -> ModBlocks.MUD_BLOCK, () -> ModItems.BUCKET_MUD);
    // acid_fluid: 2500 / 1500 / 5 / 2773; BlockFluidBase plant mit tickRate = Viskositaet / 200 = 7
    public static final Entry ACID = register("acid_fluid", "acid", 2500, 1500, 5, 2773, 2, 7,
            () -> ModBlocks.ACID_BLOCK, () -> ModItems.BUCKET_ACID);
    // toxic_fluid: 2500 / 2000 / 15 / 2773, tickRate 2000 / 200 = 10
    public static final Entry TOXIC = register("toxic_fluid", "toxic", 2500, 2000, 15, 2773, 2, 10,
            () -> ModBlocks.TOXIC_BLOCK, () -> ModItems.BUCKET_TOXIC);
    // schrabidic_fluid: 31200 / 500 / 0 / 273, tickRate 500 / 200 = 2
    public static final Entry SCHRABIDIC = register("schrabidic_fluid", "schrabidic_acid", 31200, 500, 0, 273, 2, 2,
            () -> ModBlocks.SCHRABIDIC_BLOCK, () -> ModItems.BUCKET_SCHRABIDIC_ACID);
    // volcanic_lava_fluid: 3000 / 3000 / 15 / 1300, Standard-Tickrate von BlockFluidClassic (Viskositaet/200 = 15)
    public static final Entry VOLCANIC_LAVA = register("volcanic_lava_fluid", "volcanic_lava", 3000, 3000, 15, 1300, 2, 15,
            () -> ModBlocks.VOLCANIC_LAVA_BLOCK, null);
    // rad_lava_fluid: wie Vulkanlava
    public static final Entry RAD_LAVA = register("rad_lava_fluid", "rad_lava", 3000, 3000, 15, 1300, 2, 15,
            () -> ModBlocks.RAD_LAVA_BLOCK, null);
    // sulfuric_acid_fluid: 1840 / 1000 / 0 / 273, quantaPerBlock Standard (8) = Wasser-Ausbreitung
    public static final Entry SULFURIC_ACID = register("sulfuric_acid_fluid", "sulfuric_acid", 1840, 1000, 0, 273, 1, 5,
            () -> ModBlocks.SULFURIC_ACID_BLOCK, () -> ModItems.BUCKET_SULFURIC_ACID);

    /** Laedt die Klasse, damit die Registrierungen vor {@code FLUIDS.register()} stehen. */
    public static void init() { }
}
