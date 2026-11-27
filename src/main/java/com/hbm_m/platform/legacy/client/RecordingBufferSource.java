package com.hbm_m.platform.legacy.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * [26.x] Zeichnet Vertices aus altem Immediate-Mode-Code auf (Positionen und Normalen sind beim
 * Aufzeichnen bereits durch die PoseStack transformiert) und reicht sie pro {@link RenderType} als
 * {@code submitCustomGeometry} mit Identitäts-Pose an einen {@link SubmitNodeCollector} weiter.
 *
 * <p>Nur auf dem Render-Thread benutzen.
 */
public class RecordingBufferSource implements MultiBufferSource {

    /** Collector des gerade laufenden {@code submit}-Aufrufs (für {@code BufferSource.endBatch()}). */
    private static SubmitNodeCollector currentCollector;

    private final Map<RenderType, Recorder> recorders = new IdentityHashMap<>();
    private final List<RenderType> order = new ArrayList<>();

    public static void setCurrentCollector(SubmitNodeCollector collector) {
        currentCollector = collector;
    }

    public static SubmitNodeCollector currentCollector() {
        return currentCollector;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        Recorder r = recorders.get(renderType);
        if (r == null) {
            r = new Recorder();
            recorders.put(renderType, r);
            order.add(renderType);
        }
        return r;
    }

    public boolean isEmpty() {
        for (Recorder r : recorders.values()) {
            if (r.size > 0) return false;
        }
        return true;
    }

    /** Übergibt alles Aufgezeichnete an {@code collector} und leert den Puffer. */
    public void flushTo(SubmitNodeCollector collector) {
        if (collector == null) {
            clear();
            return;
        }
        PoseStack identity = new PoseStack();
        for (RenderType rt : order) {
            Recorder r = recorders.get(rt);
            if (r == null || r.size == 0) continue;
            final Recorder snapshot = r.copy();
            collector.submitCustomGeometry(identity, rt, (pose, vc) -> snapshot.replay(vc));
        }
        clear();
    }

    protected void flushToCurrent() {
        flushTo(currentCollector);
    }

    public void clear() {
        recorders.clear();
        order.clear();
    }

    /** Flache Vertex-Aufzeichnung: x y z | argb | u v | overlay | light | nx ny nz | lineWidth. */
    static final class Recorder implements VertexConsumer {
        private static final int STRIDE = 12;
        float[] data = new float[STRIDE * 64];
        int size;

        private int base() {
            return (size - 1) * STRIDE;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if ((size + 1) * STRIDE > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            size++;
            int b = base();
            data[b] = x;
            data[b + 1] = y;
            data[b + 2] = z;
            data[b + 3] = Float.intBitsToFloat(0xFFFFFFFF);
            data[b + 4] = 0;
            data[b + 5] = 0;
            data[b + 6] = Float.intBitsToFloat(0);
            data[b + 7] = Float.intBitsToFloat(0x00F000F0);
            data[b + 8] = 0;
            data[b + 9] = 1;
            data[b + 10] = 0;
            data[b + 11] = 1;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return setColor((a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF));
        }

        @Override
        public VertexConsumer setColor(int argb) {
            if (size > 0) data[base() + 3] = Float.intBitsToFloat(argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            if (size > 0) {
                data[base() + 4] = u;
                data[base() + 5] = v;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            if (size > 0) data[base() + 6] = Float.intBitsToFloat((u & 0xFFFF) | (v & 0xFFFF) << 16);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (size > 0) data[base() + 7] = Float.intBitsToFloat((u & 0xFFFF) | (v & 0xFFFF) << 16);
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float nx, float ny, float nz) {
            if (size > 0) {
                data[base() + 8] = nx;
                data[base() + 9] = ny;
                data[base() + 10] = nz;
            }
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            if (size > 0) data[base() + 11] = width;
            return this;
        }

        Recorder copy() {
            Recorder c = new Recorder();
            c.data = Arrays.copyOf(data, Math.max(STRIDE, size * STRIDE));
            c.size = size;
            return c;
        }

        void replay(VertexConsumer vc) {
            for (int i = 0; i < size; i++) {
                int b = i * STRIDE;
                int overlay = Float.floatToRawIntBits(data[b + 6]);
                int light = Float.floatToRawIntBits(data[b + 7]);
                vc.addVertex(data[b], data[b + 1], data[b + 2])
                        .setColor(Float.floatToRawIntBits(data[b + 3]))
                        .setUv(data[b + 4], data[b + 5])
                        .setUv1(overlay & 0xFFFF, overlay >>> 16)
                        .setUv2(light & 0xFFFF, light >>> 16)
                        .setNormal(data[b + 8], data[b + 9], data[b + 10])
                        .setLineWidth(data[b + 11]);
            }
        }
    }
}
