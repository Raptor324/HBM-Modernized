// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.lib.Library + com.hbm.util.SectionKeyHash), Commit 3f9a261a.
// Nur die Schluessel-Helfer, die das Strahlungsraster braucht.

package com.hbm_m.radiation.ntmnext;

/**
 * Bit-Helfer fuer Sektions-/Chunk-Schluessel.
 *
 * <p>Sektionsschluessel haben dasselbe Layout wie {@code SectionPos.asLong} (x 22 Bit oben,
 * z 22 Bit Mitte, y 20 Bit unten), Chunkschluessel wie {@code ChunkPos.asLong}. Lokale
 * Blockindizes sind {@code (y << 8) | (z << 4) | x} wie im PalettedContainer.
 */
final class SectionKeys {

    private SectionKeys() {}

    /** BlockPos.asLong -> lokaler Index innerhalb der Sektion. */
    static int blockPosToLocal(long serialized) {
        return ((int) (serialized & 0xFL) << 8)
                | (((int) (serialized >>> 12) & 0xF) << 4)
                | ((int) (serialized >>> 38) & 0xF);
    }

    static int getLocalX(int packed) {
        return packed & 0xF;
    }

    static int getLocalY(int packed) {
        return (packed >>> 8) & 0xF;
    }

    static int getLocalZ(int packed) {
        return (packed >>> 4) & 0xF;
    }

    /** ChunkPos-Long + Sektions-Y -> SectionPos-Long. */
    static long sectionToLong(long ck, int subY) {
        return (ck << 42) | ((ck >>> 12) & 0x0000_03FF_FFF0_0000L) | (((long) subY) & 0xFFFFFL);
    }

    static long setSectionY(long key, int subY) {
        return (key & ~0xFFFFFL) | (((long) subY) & 0xFFFFFL);
    }

    static long shiftSectionX(long key, int dx) {
        return (key & ~(0x3FFFFFL << 42)) | (((key >>> 42) + dx & 0x3FFFFFL) << 42);
    }

    static long shiftSectionZ(long key, int dz) {
        return (key & ~(0x3FFFFFL << 20)) | ((((key >>> 20) & 0x3FFFFFL) + dz & 0x3FFFFFL) << 20);
    }

    /** SectionPos-Long -> ChunkPos-Long. */
    static long sectionToChunkLong(long sck) {
        return (((sck << 22) >> 10) & 0xFFFF_FFFF_0000_0000L) | ((sck >> 42) & 0xFFFF_FFFFL);
    }

    /** Mischhash fuer offene Adressierung (SectionKeyHash). */
    static int hash(long z) {
        z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
        return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
    }
}
