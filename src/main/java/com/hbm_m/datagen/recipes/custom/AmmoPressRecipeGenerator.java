package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * Port von {@code AmmoPressRecipes} (1.7.10 Original, 89 Rezepte). Von den 89 Original-Rezepten
 * sind in diesem Port NUR 2 vollstaendig abbildbar - der Rest haengt fast vollstaendig von
 * Infrastruktur ab, die in diesem Port (noch) nicht existiert:
 * <ul>
 *   <li>Huelsen-Items ({@code ModItems.casing}+{@code EnumCasingType}: klein/gross/Stahl/
 *   Schrotpatrone/...) - keine Entsprechung registriert.</li>
 *   <li>Rauchloses-Pulver-Staub ({@code ANY_SMOKELESS.dust()}) - existiert nicht.</li>
 *   <li>Generische "beliebiger Kunststoff"/"beliebiger Sprengstoff"-Ingot-Platzhalter
 *   ({@code ANY_PLASTIC}/{@code ANY_HIGHEXPLOSIVE}) - existieren nicht.</li>
 *   <li>Fluid-Inputs (Diesel/Gas/Balefire als Item-Bucket) - in diesem Rezeptsystem nicht
 *   abbildbar (Ammo Press hat keine Fluid-Slots).</li>
 *   <li>Die meisten Ziel-Munitionsitems selbst ({@code EnumAmmo}-Metadaten-System des Originals,
 *   ~60 Varianten: .357/.44/.22/.45/7.62mm/10-Gauge/40mm-Granaten/Raketen/Napalm/Nuklear-Sprengkoepfe/
 *   etc.) wurden in diesem Port nie als einzelne Items angelegt - nur eine Handvoll Turm-Munition
 *   existiert (AMMO_9MM_xx, AMMO_50_xx, AMMO_556_xx, AMMO_TAU_URANIUM, AMMO_FLAME_DIESEL).</li>
 * </ul>
 * Diese Infrastruktur vollstaendig nachzuruesten (Huelsen-Item-System, Pulver-Item, Platzhalter-
 * Ingots, ~60 neue Munitions-Items) waere ein eigenes, um Groessenordnungen umfangreicheres
 * Vorhaben als der Ammo-Press-Maschinen-Port selbst - siehe gleiche Begruendung wie beim
 * Meilenstein-Auszahlungssystem in {@code MachineAnnihilatorBlockEntity}. Die 2 Rezepte, die
 * ausschliesslich bereits vorhandene Items verwenden, sind unten 1:1 uebernommen.
 */
public class AmmoPressRecipeGenerator {

    public static void generate(Consumer<FinishedRecipe> writer) {

        // Tauon-Turm-Uran-Munition (EnumAmmo.TAU_URANIUM) - 1:1 aus dem Original.
        AmmoPressRecipeBuilder.ammoPressRecipe(new ItemStack(ModItems.AMMO_TAU_URANIUM.get(), 16))
                .slot(1, ModItems.PLATE_LEAD.get())
                .slot(4, ModItems.getIngot(ModIngots.URANIUM238).get())
                .slot(7, ModItems.PLATE_LEAD.get())
                .save(writer, "ammo_press/tau_uranium");

        // Wolfram-Spule (EnumAmmo.COIL_TUNGSTEN) - 1:1 aus dem Original.
        AmmoPressRecipeBuilder.ammoPressRecipe(new ItemStack(ModItems.COIL_TUNGSTEN.get(), 4))
                .slot(4, ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .save(writer, "ammo_press/coil_tungsten");
    }
}
