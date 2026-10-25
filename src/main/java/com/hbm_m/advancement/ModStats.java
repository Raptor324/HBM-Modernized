package com.hbm_m.advancement;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

/**
 * Eigene Statistiken des Originals ({@code MainRegistry.statBullets} = "stat.bullets", abgefeuerte Kugeln;
 * {@code MainRegistry.statLegendary} = "stat.ntmLegendary", am Sockel hergestellte legendaere Gegenstaende).
 */
public final class ModStats {

    private ModStats() { }

    public static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(RefStrings.MODID, Registries.CUSTOM_STAT);

    private static final ResourceLocation BULLETS_ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "bullets");
    public static final RegistrySupplier<ResourceLocation> BULLETS_KEY = STATS.register("bullets", () -> BULLETS_ID);

    /** Stat-Objekt (legt den Eintrag in {@code Stats.CUSTOM} beim ersten Zugriff an). */
    public static final java.util.function.Supplier<Stat<ResourceLocation>> BULLETS = () -> Stats.CUSTOM.get(BULLETS_KEY.get(), StatFormatter.DEFAULT);

    private static final ResourceLocation LEGENDARY_ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "legendary");
    public static final RegistrySupplier<ResourceLocation> LEGENDARY_KEY = STATS.register("legendary", () -> LEGENDARY_ID);

    /** Original {@code statLegendary}: vergibt {@code BlockPedestal} an alle Spieler im Umkreis von 50 Bloecken. */
    public static final java.util.function.Supplier<Stat<ResourceLocation>> LEGENDARY = () -> Stats.CUSTOM.get(LEGENDARY_KEY.get(), StatFormatter.DEFAULT);

    public static void init() {
        STATS.register();
    }
}
