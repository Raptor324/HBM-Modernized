package com.hbm_m.main;

/**
 * Gemaelde-Varianten. Ab 1.21 datengetrieben ({@code data/hbm_m/painting_variant/*.json}), in 1.20.1
 * noch ein fest eingebautes Register - ohne Anmeldung hier scheitert dort der Tag
 * {@code minecraft:placeable} ("missing following references: hbm_m:bober1").
 * Groessen wie im JSON (Bloecke) mal 16 Pixel.
 */
public final class ModPaintings {

    private ModPaintings() {}

    //? if < 1.21.1 {
    public static final dev.architectury.registry.registries.DeferredRegister<net.minecraft.world.entity.decoration.PaintingVariant> PAINTINGS =
            dev.architectury.registry.registries.DeferredRegister.create(com.hbm_m.lib.RefStrings.MODID, net.minecraft.core.registries.Registries.PAINTING_VARIANT);

    public static final dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.entity.decoration.PaintingVariant> BOBER1 =
            PAINTINGS.register("bober1", () -> new net.minecraft.world.entity.decoration.PaintingVariant(3 * 16, 2 * 16));
    //?}

    public static void init() {
        //? if < 1.21.1 {
        PAINTINGS.register();
        //?}
    }
}
