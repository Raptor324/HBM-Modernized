package com.hbm_m.platform.legacy.client.model;

import net.minecraft.resources.Identifier;

/**
 * [26.x] Ersatz für das entfernte {@code net.minecraft.client.resources.model.ModelResourceLocation}.
 * Dient nur noch als Schlüssel für {@link NMeshPartRegistry}; Varianten werden ignoriert.
 */
public record ModelResourceLocation(Identifier id, String variant) {

    public static ModelResourceLocation inventory(Identifier id) {
        return new ModelResourceLocation(id, "inventory");
    }

    public static ModelResourceLocation standalone(Identifier id) {
        return new ModelResourceLocation(id, "standalone");
    }
}
