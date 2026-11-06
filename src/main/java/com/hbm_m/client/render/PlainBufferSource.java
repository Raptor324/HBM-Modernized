package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Direct subclass of the vanilla {@link MultiBufferSource.BufferSource} - bypassing
 * the {@code MultiBufferSource.immediate/immediateWithBuffers} factories.
 *
 * WHY: ImmediatelyFast redirects those factories and swaps ANY source created by
 * them for its own BatchableBufferSource, whose no-arg endBatch() flushes all
 * registered layers unconditionally - and fails with "Sorting state uninitialized"
 * on an EMPTY RenderType batch with sortOnUpload=true (our nuke_clouds/nuke_flash,
 * when the near/far filter or flash-only mode wrote no vertices into the type).
 * Therefore ALL of our custom sortOnUpload render types must be drawn only through
 * a source of this class (see ParticleEngineNT.buffer()).
 *
 * SECOND (main) point: on 1.20.1 we do NOT use the vanilla batch bookkeeping
 * (lastState + startedBuffers) but keep our own symmetric one: begin(X) at the
 * first getBuffer(X), RenderType.end(X) strictly paired - in endCurrentBatch().
 * The vanilla machinery has desync paths (e.g. endBatch(rt) with lastState != rt
 * is simply skipped via startedBuffers.remove() == false, leaving lastState
 * pointing at an already-closed batch; after such a skip a subsequent end() may
 * see the previous batch's sorting state - hence "Sorting state uninitialized").
 * With a symmetric pair the builder state always agrees with the type: end() is
 * called only for the type whose begin is open, and sorting (per-pixel, by
 * distance to camera) works as in vanilla. An empty batch is not drawn at all
 * (endOrDiscardIfEmpty - nothing to lose, zero vertices).
 */
public class PlainBufferSource extends MultiBufferSource.BufferSource {

    //? if < 1.21.1 {
    /** The type whose begin() is currently open on the shared builder; null = none. */
    private RenderType buildingType;

    public PlainBufferSource(com.mojang.blaze3d.vertex.BufferBuilder sharedBuffer) {
        super(sharedBuffer, com.google.common.collect.ImmutableMap.of());
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (this.buildingType != renderType) {
            endCurrentBatch();
            this.builder.begin(renderType.mode(), renderType.format());
            this.buildingType = renderType;
        }
        return this.builder;
    }

    @Override
    public void endBatch(RenderType renderType) {
        if (this.buildingType == renderType) {
            endCurrentBatch();
        }
    }

    @Override
    public void endLastBatch() {
        endCurrentBatch();
    }

    @Override
    public void endBatch() {
        endCurrentBatch();
    }

    private void endCurrentBatch() {
        RenderType type = this.buildingType;
        if (type == null) {
            return;
        }
        this.buildingType = null;
        if (this.builder.isCurrentBatchEmpty()) {
            // Empty batch: reset the builder without drawing (and without the
            // sorting machinery - empty sortOnUpload batches were exactly the
            // source of "Sorting state uninitialized" under ImmediatelyFast).
            this.builder.endOrDiscardIfEmpty();
        } else {
            // Full vanilla draw path: setQuadSorting (if the type is sortable)
            // -> end -> setupRenderState -> drawWithShader.
            type.end(this.builder, com.mojang.blaze3d.systems.RenderSystem.getVertexSorting());
        }
    }
    //?} else {
    /*// 1.21.1: the vanilla BufferSource already keeps a separate BufferBuilder
    // per type (startedBuilders) and sorts via MeshData.sortQuads - the
    // vulnerable shared machinery of 1.20.1 does not exist here; a plain
    // subclass is enough.
    // IMPORTANT: fixedBuffers must NOT be Collections.emptySortedMap() - under
    // the hood it is a TreeMap whose get() casts the key to Comparable, and
    // custom RenderTypes are not Comparable (crash "CompositeRenderType cannot
    // be cast to Comparable" on the first getBuffer). The vanilla immediate()
    // factory uses an empty fastutil map - we do the same.
    public PlainBufferSource(com.mojang.blaze3d.vertex.ByteBufferBuilder sharedBuffer) {
        super(sharedBuffer, it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps.emptyMap());
    }
    *///?}
}
