package com.hbm_m.block.custom.machines.anvils;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * Represents the legacy tiers from the original 1.7.10 anvils so we can map
 * gameplay restrictions and GUIs onto the modern block set.
 */
public enum AnvilTier {
    IRON(1),
    STEEL(2),
    OIL(3),
    NUCLEAR(4),
    RBMK(5),
    FUSION(6),
    PARTICLE(7),
    GERALD(8),
    MURKY(1_916_169);

    private final int legacyId;

    AnvilTier(int legacyId) {
        this.legacyId = legacyId;
    }

    public int getLegacyId() {
        return legacyId;
    }

    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
    }

    public String getTranslationKey() {
        return "tier.hbm_m.anvil." + name().toLowerCase(Locale.ROOT);
    }

    public static AnvilTier fromLegacyId(int legacyId) {
        return Arrays.stream(values())
                .filter(tier -> tier.legacyId == legacyId)
                .findFirst()
                .orElse(IRON);
    }
}

