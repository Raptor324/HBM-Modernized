package com.hbm_m.platform.legacy;

import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

/**
 * [26.x] Registry-Schlüssel für Stellen, an denen 26.x statt eines Namens einen {@link ResourceKey} verlangt
 * (z. B. {@code EntityType.Builder.build}). Regel {@code mc26_entity_build}.
 */
public final class RegIds {

    private RegIds() {}

    /** Alt: {@code build("name")} bzw. {@code build("hbm_m:name")}. */
    public static ResourceKey<EntityType<?>> entity(String name) {
        Identifier id = name.indexOf(':') >= 0 ? Identifier.parse(name) : Identifier.fromNamespaceAndPath(RefStrings.MODID, name);
        return ResourceKey.create(Registries.ENTITY_TYPE, id);
    }
}
