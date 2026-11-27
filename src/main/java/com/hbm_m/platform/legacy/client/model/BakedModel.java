package com.hbm_m.platform.legacy.client.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [26.x] Schmaler Ersatz für das in 26.x entfernte {@code net.minecraft.client.resources.model.BakedModel}.
 *
 * <p>Nur noch Datenträger für die Nucleus-Renderer (Teile, Quads für CPU-Pfade, Partikel-Sprite).
 * Das Chunk-/Item-Rendering von Minecraft läuft auf 26.x über {@code NMeshModelLoader26}.
 * Stonecutter ersetzt den Import (Regel {@code mc26_bakedmodel}).
 */
public interface BakedModel {

    List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand);

    default boolean useAmbientOcclusion() { return true; }

    default boolean isGui3d() { return true; }

    default boolean usesBlockLight() { return true; }

    default boolean isCustomRenderer() { return false; }

    default @Nullable TextureAtlasSprite getParticleIcon() { return null; }

    default ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }

    BakedModel EMPTY = (state, side, rand) -> List.of();
}
