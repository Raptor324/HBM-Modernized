package com.hbm_m.platform.legacy.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * [26.x] Ersatz für das alte {@code net.minecraft.client.resources.model.Material} (Atlas + Textur).
 * 26.x hat {@code sprite.Material} ohne Atlas; die Rüstungs-Layer brauchen aber weiter {@link #sprite()}.
 */
public record Material(Identifier atlasLocation, Identifier texture) {

    public TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(atlasId()).getSprite(texture);
    }

    /** Alte Atlas-Textur-Pfade ({@code textures/atlas/blocks.png}) auf die 26.x-Atlas-IDs abbilden. */
    private Identifier atlasId() {
        String p = atlasLocation.getPath();
        if (p.startsWith("textures/atlas/") && p.endsWith(".png")) {
            return Identifier.fromNamespaceAndPath(atlasLocation.getNamespace(),
                    p.substring("textures/atlas/".length(), p.length() - 4));
        }
        return atlasLocation;
    }
}
