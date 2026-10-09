package com.hbm_m.platform;

/**
 * Nur 1.21.1: Block-Codecs fuer {@code codec()} (seit 1.20.5 abstrakt in BaseEntityBlock, BushBlock,
 * FallingBlock ...). Fuer Bloecke mit Konstruktor {@code (Properties)} wird direkt
 * {@code simpleCodec(X::new)} benutzt; fuer alle anderen dieser Platzhalter. Mod-Bloecke werden nie
 * ueber den Codec (de)serialisiert - der Codec dient nur der Typ-Registrierung.
 */
public final class BlockCodecs {
    private BlockCodecs() {}

    //? if >= 1.21.1 {
    /*public static <B> com.mojang.serialization.MapCodec<B> unsupported(Class<B> type) {
        return com.mojang.serialization.MapCodec.unit(() -> {
            throw new UnsupportedOperationException("Kein Codec fuer " + type.getName());
        });
    }
    *///?}
}
