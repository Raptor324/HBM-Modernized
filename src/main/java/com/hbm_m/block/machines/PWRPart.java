package com.hbm_m.block.machines;

/** Kennung der PWR-Bauteile ({@link PWRPartBlock} und {@link PWRPartBlock.Pillar}). */
public interface PWRPart {

    enum Kind { FUEL, CONTROL, CHANNEL, HEATEX, HEATSINK, NEUTRON_SOURCE, CASING, REFLECTOR, PORT;

        /** Original: {@code isValidCasing}. */
        public boolean isCasing() { return this == CASING || this == REFLECTOR || this == PORT; }
    }

    Kind getKind();
}
