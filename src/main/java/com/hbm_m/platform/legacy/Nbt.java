package com.hbm_m.platform.legacy;

/**
 * [26.x] Träger für den alten NBT-Typ-Parameter von {@code getList(key, type)} / {@code contains(key, type)},
 * damit die Stonecutter-Regeln {@code mc26_nbt_list}/{@code mc26_nbt_contains} umkehrbar bleiben.
 * Der Typ wird in 26.x nicht mehr geprüft.
 */
public final class Nbt {

    private Nbt() {}

    public static String k(String key, int legacyType) {
        return key;
    }
}
