package com.hbm_m.item.machine;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod;

import net.minecraft.world.item.Item;

/**
 * Nachschlagewerk fuer die Brennstaebe des Uranmeilers.
 *
 * <p>Im Original sind das sieben Metadaten <b>eines</b> Gegenstands, und {@code turnsInto} zeigt
 * schlicht auf die naechste Metadatenzahl. Dieser Port registriert sieben eigene Gegenstaende, also
 * braucht es diese Zuordnung, um aus dem Enum wieder den Gegenstand zu bekommen.</p>
 */
public final class ModPileRods {

    private ModPileRods() {}

    public static Item of(EnumPileRod rod) {
        return switch (rod) {
            case RA226BE -> ModItems.PILE_ROD_RA226BE.get();
            case PO210BE -> ModItems.PILE_ROD_PO210BE.get();
            case ZR      -> ModItems.PILE_ROD_ZR.get();
            case NU      -> ModItems.PILE_ROD_NU.get();
            case PU239   -> ModItems.PILE_ROD_MK2_PU239.get();
            case RGP     -> ModItems.PILE_ROD_RGP.get();
            case WASTE   -> ModItems.PILE_ROD_WASTE.get();
        };
    }
}
