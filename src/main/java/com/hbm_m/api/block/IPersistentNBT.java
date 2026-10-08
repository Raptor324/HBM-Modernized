package com.hbm_m.api.block;

import net.minecraft.nbt.CompoundTag;

/**
 * Port von {@code com.hbm.tileentity.IPersistentNBT}: Maschinen, die beim Abbau ihren Inhalt (Tanks, Ladung) im
 * Gegenstand mitnehmen. {@link #writeNBT} schreibt die mitzunehmenden Werte im eigenen Speicherformat der
 * Blockentitaet (leer lassen = nichts mitnehmen). Die Loot-Funktion {@code hbm_m:persistent_nbt} legt sie als
 * {@code BlockEntityTag} ab; beim Setzen laedt Vanilla sie ueber {@code load} zurueck (Original {@code restoreData}).
 */
public interface IPersistentNBT {

    String NBT_PERSISTENT_KEY = "persistent";

    void writeNBT(CompoundTag nbt);
}
