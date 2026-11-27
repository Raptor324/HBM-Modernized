package com.hbm_m.platform.legacy;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * [26.x] Blöcke haben seit 26.x kein {@code codec()} mehr, und {@code BlockBehaviour.simpleCodec}
 * ist weg. Die alten {@code CODEC}-Felder bleiben im Quelltext (für 1.21.1); Stonecutter leitet
 * {@code simpleCodec(...)} hierher um und entfernt das {@code @Override} an {@code codec()}
 * (Regeln {@code mc26_simplecodec} / {@code mc26_codec_override}). Der Codec wird nie benutzt.
 */
public final class BlockCodecs {

    private BlockCodecs() {}

    public static <B> MapCodec<B> legacySimpleCodec(Function<BlockBehaviour.Properties, B> factory) {
        return MapCodec.unit(() -> {
            throw new UnsupportedOperationException("Block codecs do not exist on Minecraft 26.x");
        });
    }
}
