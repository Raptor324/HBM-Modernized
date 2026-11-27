package com.hbm_m.platform.legacy.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * [26.x] Ersatz für das entfernte {@code net.minecraft.client.renderer.MultiBufferSource}.
 *
 * <p>26.x zeichnet nicht mehr sofort, sondern sammelt Aufträge über {@code SubmitNodeCollector}.
 * Stonecutter biegt den alten Import auf dieses Interface um (Regel {@code mc26_multibuffersource});
 * die Implementierung {@link RecordingBufferSource} zeichnet die Vertices auf und reicht sie als
 * {@code submitCustomGeometry} an den Collector weiter.
 */
public interface MultiBufferSource {

    VertexConsumer getBuffer(RenderType renderType);

    /** Gegenstück zu {@code MultiBufferSource.BufferSource}: hier nur ein aufzeichnender Puffer. */
    class BufferSource extends RecordingBufferSource {
        public void endBatch() {
            flushToCurrent();
        }

        public void endBatch(RenderType renderType) {
            flushToCurrent();
        }

        public void endLastBatch() {
            flushToCurrent();
        }
    }
}
