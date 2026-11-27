package com.hbm_m.platform.legacy.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [26.x] Teile-Container für Nucleus, gebaut von {@link NMeshPartRegistry} direkt aus {@code NMeshData}.
 * Ersetzt das gleichnamige BakedModel aus {@code com.hbm_m.client.model} (auf 26.x nicht im Build);
 * die Unterklassen bilden die alten Modelltypen ab, damit {@code instanceof}-Prüfungen der Renderer
 * weiter funktionieren.
 */
public abstract class AbstractMultipartBakedModel implements BakedModel {

    protected final Map<String, BakedModel> parts;
    protected final ItemTransforms transforms;
    protected final Identifier modelId;
    @Nullable private TextureAtlasSprite particle;
    private final String[] partNames;

    protected AbstractMultipartBakedModel(Identifier modelId, Map<String, BakedModel> parts, ItemTransforms transforms) {
        this.modelId = modelId;
        this.parts = Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        this.transforms = transforms;
        this.partNames = parts.keySet().stream().sorted().toArray(String[]::new);
    }

    public Identifier getModelId() { return modelId; }

    public String getModelKey() { return modelId.toString(); }

    public @Nullable BakedModel getPart(String name) { return parts.get(name); }

    public Map<String, BakedModel> getParts() { return parts; }

    public Map<String, BakedModel> getPartModels() { return parts; }

    public String[] getPartNames() { return partNames; }

    public Set<String> getPartNameSet() { return parts.keySet(); }

    public void setParticleIcon(@Nullable TextureAtlasSprite sprite) { this.particle = sprite; }

    public void clearCaches() {}

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> out = new ArrayList<>();
        for (BakedModel part : parts.values()) out.addAll(part.getQuads(state, side, rand));
        return out;
    }

    @Override
    public @Nullable TextureAtlasSprite getParticleIcon() {
        if (particle != null) return particle;
        for (String key : new String[] { "Base", "base", "frame", "Frame", "main", "Main" }) {
            BakedModel p = parts.get(key);
            if (p != null && p.getParticleIcon() != null) return p.getParticleIcon();
        }
        for (BakedModel p : parts.values()) if (p.getParticleIcon() != null) return p.getParticleIcon();
        return null;
    }

    @Override
    public ItemTransforms getTransforms() { return transforms; }
}
