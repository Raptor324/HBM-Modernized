package com.hbm_m.client.loader;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.ModelHelper;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Обёртка над canonical-запечённой частью OBJ: отдаёт квады с UV, аффинно
 * ремапнутыми из области исходного спрайта в область спрайта скина.
 * Основа дедупликации текстурных скинов дверей — canonical-квады хранятся в
 * одном экземпляре, варианты не хранят ничего, кроме пары спрайтов.
 * <p>
 * Ремап выполняется на каждый вызов {@link #getQuads} БЕЗ кэша: вызовы редкие
 * (одноразовое строительство VBO части, item-квады раз в секунду). Часть
 * отдаёт только квады — методы модели делегируют canonical-части.
 */
public final class RemappedPartModel implements BakedModel {

    private final BakedModel canonical;
    private final TextureAtlasSprite fromSprite;
    private final TextureAtlasSprite toSprite;

    public RemappedPartModel(BakedModel canonical, TextureAtlasSprite fromSprite, TextureAtlasSprite toSprite) {
        this.canonical = canonical;
        this.fromSprite = fromSprite;
        this.toSprite = toSprite;
    }

    public BakedModel canonical() {
        return canonical;
    }

    public TextureAtlasSprite fromSprite() {
        return fromSprite;
    }

    public TextureAtlasSprite toSprite() {
        return toSprite;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand) {
        return ModelHelper.remapQuadUvs(canonical.getQuads(state, side, rand), fromSprite, toSprite);
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return canonical.getTransforms();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return canonical.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return canonical.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return canonical.usesBlockLight();
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return canonical.getParticleIcon();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return canonical.getOverrides();
    }

    // Абстрактен и на Forge 1.20.1, и на NeoForge 1.21.1 — без stonecutter-гейта.
    @Override
    public boolean isCustomRenderer() {
        return canonical.isCustomRenderer();
    }
}
