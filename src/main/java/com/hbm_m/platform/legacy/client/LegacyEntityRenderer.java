package com.hbm_m.platform.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * [26.x] Brücke für Entity-Renderer im alten Stil
 * ({@code render(entity, yaw, partialTick, poseStack, buffers, light)} + {@code getTextureLocation}).
 * Stonecutter ersetzt {@code extends EntityRenderer<X>} durch {@code extends LegacyEntityRenderer<X>}
 * (Regel {@code mc26_entity_renderer}).
 *
 * <p>Wie bei {@link LegacyBer} hält der Render-State die Entity; Extraktion und Submit laufen auf dem
 * Render-Thread.
 */
public abstract class LegacyEntityRenderer<T extends Entity> extends EntityRenderer<T, LegacyEntityRenderer.State<T>> {

    protected LegacyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Alter Einstiegspunkt. Der Standard tut nichts (früher: Namensschild). */
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
    }

    /** Alte Textur-Abfrage; wird von 26.x selbst nicht mehr benutzt. */
    public Identifier getTextureLocation(T entity) {
        return null;
    }

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void extractRenderState(T entity, State<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partialTick = partialTick;
        state.yaw = entity.getYRot(partialTick);
    }

    @Override
    public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        T entity = state.entity;
        if (entity != null) {
            RecordingBufferSource buffers = new RecordingBufferSource();
            SubmitNodeCollector previous = RecordingBufferSource.currentCollector();
            RecordingBufferSource.setCurrentCollector(collector);
            try {
                render(entity, state.yaw, state.partialTick, poseStack, buffers, state.lightCoords);
            } finally {
                buffers.flushTo(collector);
                RecordingBufferSource.setCurrentCollector(previous);
                state.entity = null;
            }
        }
        super.submit(state, poseStack, collector, camera);
    }

    public static class State<T extends Entity> extends EntityRenderState {
        public T entity;
        public float partialTick;
        public float yaw;
    }
}
