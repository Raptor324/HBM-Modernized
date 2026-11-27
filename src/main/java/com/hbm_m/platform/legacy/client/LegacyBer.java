package com.hbm_m.platform.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * [26.x] Brücke für Block-Entity-Renderer im alten Stil
 * ({@code render(be, partialTick, poseStack, buffers, light, overlay)}). Stonecutter ersetzt
 * {@code BlockEntityRenderer<X>} durch {@code LegacyBer<X>} (Regel {@code mc26_ber}).
 *
 * <p>Der Render-State hält eine Referenz auf das BE. Extraktion und Submit laufen in 26.3 beide auf dem
 * Render-Thread, das ist also sicher. Neue Renderer (und Nucleus) sollten besser direkt einen eigenen
 * Render-State extrahieren.
 */
public interface LegacyBer<T extends BlockEntity> extends BlockEntityRenderer<T, LegacyBer.State<T>> {

    void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                int packedLight, int packedOverlay);

    @Override
    default State<T> createRenderState() {
        return new State<>();
    }

    @Override
    default void extractRenderState(T blockEntity, State<T> state, float partialTick, Vec3 cameraPos,
                                    ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, breakProgress);
        state.blockEntity = blockEntity;
        state.partialTick = partialTick;
    }

    @Override
    default void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        T be = state.blockEntity;
        if (be == null) return;
        RecordingBufferSource buffers = new RecordingBufferSource();
        SubmitNodeCollector previous = RecordingBufferSource.currentCollector();
        RecordingBufferSource.setCurrentCollector(collector);
        try {
            render(be, state.partialTick, poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY);
        } finally {
            buffers.flushTo(collector);
            RecordingBufferSource.setCurrentCollector(previous);
            state.blockEntity = null;
        }
    }

    final class State<T extends BlockEntity> extends BlockEntityRenderState {
        public T blockEntity;
        public float partialTick;
    }
}
