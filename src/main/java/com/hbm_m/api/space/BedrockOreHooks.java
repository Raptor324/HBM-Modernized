package com.hbm_m.api.space;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * W-R8B-1: koerpergebundene Bedrock-Erze (NTM Space {@code CelestialBedrockOre}) in den HBM-Stellen, die das rohe
 * Grundgestein-Erz ({@code hbm_m:bedrock_ore_base}) erzeugen, beschreiben, schlaemmen und scannen. Alle Methoden haben
 * einen "nicht zustaendig"-Standard, mit dem HBM exakt das bisherige Verhalten zeigt. Setzen:
 * {@link SpaceApi#setBedrockOreHooks} (Mod-Konstruktor, Client und Server).
 *
 * <p>Die Weiterverarbeitung (Zentrifuge, Kristallisator, Elektrolyse, Lichtbogen, Pyro-, Kombinationsofen) laeuft ueber
 * die Gegenstaende, die {@link #slopperOutput} liefert - deren Rezepte meldet das Addon selbst an (eigene Items je Sorte).
 */
public interface BedrockOreHooks {

    BedrockOreHooks DEFAULT = new BedrockOreHooks() { };

    /**
     * Original {@code ItemBedrockOreBase.setOreAmount(world, stack, x, z, mult)} (Bagger/Excavator): Koerper + Mengen je
     * Sorte ins Gegenstands-NBT schreiben. {@code true} = erledigt, HBM schreibt nichts; {@code false} = HBM-Standard.
     */
    default boolean setOreAmount(ServerLevel level, ItemStack stack, int x, int z, double mult) {
        return false;
    }

    /**
     * Original {@code ItemBedrockOreBase.addInformation} ("Mined on: Koerper" + Sorten des Koerpers). {@code true} =
     * Addon hat die Zeilen angehaengt, HBM fuegt keine eigenen hinzu. Laeuft auf dem Client.
     */
    default boolean appendBaseOreTooltip(ItemStack stack, List<Component> tooltip) {
        return false;
    }

    /**
     * Erzschlammer: Mengen je Sorte des Eingangs (Original {@code getOreBody} + {@code getOreAmount} je Sorte des
     * Koerpers), Reihenfolge = Ausgabereihenfolge (Original {@code type.index}). {@code null} = HBM-Standard.
     */
    @Nullable
    default Map<ResourceLocation, Double> oreAmounts(ItemStack stack) {
        return null;
    }

    /** Erzschlammer-Ausgabe einer Addon-Sorte (Original {@code ItemBedrockOreNew.make(BASE, type)}), leer = keine. */
    default ItemStack slopperOutput(ResourceLocation oreType) {
        return ItemStack.EMPTY;
    }

    /** Eine Zeile des Erzdichtescanners: Name der Sorte, Dichte, Infozeilen-Index (Original {@code 777 + type.index}). */
    record ScanLine(Component name, double level, int index) { }

    /**
     * Original {@code ItemOreDensityScanner.onUpdate}: Sorten des Koerpers an x/z. {@code null} = HBM-Standard (sechs
     * Erdsorten).
     */
    @Nullable
    default List<ScanLine> scan(ServerLevel level, int x, int z) {
        return null;
    }

    /** Index der Gesamtzeile (Original {@code 777 + getTotalTypeCount()}), nur wenn {@link #scan} nicht null liefert. */
    default int scanSummaryIndex() {
        return 6;
    }

    /** Bohrfluessigkeit samt Menge (Original {@code FluidStack}). */
    record BoreFluid(Fluid fluid, int amountMb) { }

    /**
     * Original {@code WorldProviderCelestial.getBedrockAcid()}: ersetzt im Erzdichtescanner die Bohrfluessigkeit der
     * Stufe (auch Stufe 1). null = Standard.
     */
    @Nullable
    default BoreFluid bedrockAcid(Level level) {
        return null;
    }
}
