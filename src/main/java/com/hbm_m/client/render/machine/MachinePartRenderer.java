package com.hbm_m.client.render.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.InstancedGlCompat;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * GPU holder of a SINGLE machine part, created automatically by the engine.
 * <p>
 * Degradation chain per draw (in attempt order):
 * <ol>
 *   <li>MDI (inside {@link InstancedStaticPartRenderer#flush} - automatic,
 *       if GL43/ARB is available and there is no shader pack);</li>
 *   <li>hardware instancing ({@code glDrawElementsInstanced});</li>
 *   <li>per-BE VBO ({@link SingleMeshVboRenderer});</li>
 *   <li>vanilla immediate ({@code putBulkData}) - also forced via
 *       {@code forceVanillaImmediatePath}.</li>
 * </ol>
 * Geometry is built once, lazily, on the render thread; before that calls are retried.
 */
final class MachinePartRenderer {

    private final String key;
    private final String partName;
    private final boolean dynamic;

    @Nullable private InstancedStaticPartRenderer instanced;
    @Nullable private SingleMeshVboRenderer single;
    @Nullable private List<BakedQuad> quads;
    private boolean attempted;

    MachinePartRenderer(String key, String partName, boolean dynamic) {
        this.key = key;
        this.partName = partName;
        this.dynamic = dynamic;
    }

    /** true if a build attempt has already been made (successful or not) - do not call the quad resolver again. */
    boolean isAttempted() { return attempted; }

    boolean matches(MachineSpec.PartDef<?> part, String cacheKey) {
        return this.key.equals(cacheKey);
    }

    String key() { return key; }

    /** Lazy build. Off the render thread - simply defer the attempt. */
    void ensureBuilt(@Nullable BakedModel partModel, @Nullable List<BakedQuad> dynamicQuadsIn) {
        if (attempted) return;
        if (!RenderSystem.isOnRenderThread()) return;

        this.attempted = true;
        SingleMeshVboRenderer.VboData data = null;
        if (dynamic) {
            List<BakedQuad> resolved = (dynamicQuadsIn == null || dynamicQuadsIn.isEmpty())
                    ? List.of() : dynamicQuadsIn;
            this.quads = resolved;
            if (!resolved.isEmpty()) {
                data = PartGeometry.buildVboDataFromQuads(resolved, partName);
                this.single = MeshRenderCache.getOrCreateRendererFromQuadList(key, resolved);
            }
        } else {
            PartGeometry geo = (partModel != null)
                    ? MeshRenderCache.getOrCompilePartGeometry(key, partModel)
                    : PartGeometry.EMPTY;
            this.quads = geo.solidQuads();
            if (!geo.isEmpty()) {
                data = geo.toVboData(partName);
                this.single = MeshRenderCache.getOrCreateRenderer(key, partModel);
            }
        }

        if (data != null) {
            if (InstancedGlCompat.supportsInstancedAttributeDivisor()) {
                InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, quads);
                r.setMdiTraceTag(key);
                this.instanced = r;
            } else {
                data.close();
            }
        }
    }

    boolean hasGeometry() {
        return instanced != null || single != null || (quads != null && !quads.isEmpty());
    }

    InstancedStaticPartRenderer instanced() { return instanced; }

    /**
     * Adds the current frame instance or draws via a fallback path.
     *
     * @param poseStack  stack with the block transform + part animator (composed pose)
     * @param sharedLight shared 8-corner light sample of the machine (or null)
     * @param uvRect {u0,v0,du,dv} remap of sprite-local VBO -> atlas, or null (atlas UVs).
     *               Non-null means the cached quads are normalized - the single-VBO path
     *               (no remap in the shader) is replaced by the immediate fallback.
     * @param tint {r,g,b,a} per-instance color multiplier or null (white). A non-white
     *               tint on the single-VBO path also degrades to immediate (the
     *               block_lit shader does not read tint); RGB may be &gt; 1 - overbright.
     */
    void enqueue(PoseStack poseStack,
                 int packedLight, BlockPos blockPos, BlockEntity blockEntity,
                 @Nullable MultiBufferSource bufferSource, @Nullable float[] sharedLight,
                 @Nullable float[] uvRect, @Nullable float[] tint, @Nullable float[] gradFalloff) {
        // Diagram capture renders BEs into a bound offscreen FBO with a fake ortho projection;
        // deferred paths (instancing/single-VBO) flush only in the main pass, so machines would
        // vanish from the sketch - draw immediately into the shared buffer source instead.
        if (ClientRenderFlags.forceVanillaImmediate()
                || com.hbm_m.compat.simulated.DiagramRenderCompat.isRenderingDiagram()) {
            noteShadowFallback("forceVanillaImmediate/diagram");
            renderQuadsFallback(poseStack, packedLight, blockEntity, bufferSource, uvRect, tint);
            return;
        }
        if (instanced != null && instanced.isInitialized() && ClientRenderFlags.useInstancedBatching()) {
            instanced.addInstance(poseStack, packedLight, blockPos, blockEntity, bufferSource, sharedLight, uvRect, tint, gradFalloff);
            return;
        }
        if (uvRect == null && isWhiteTint(tint) && single != null) {
            single.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            return;
        }
        // uvRect != null: the single-VBO path keeps normalized UVs, and block_lit_simple
        // cannot remap them. Non-white tint: block_lit does not read tint. Both cases -
        // degrade to immediate (UV remap into the atlas + r/g/b/a into the vertices).
        // Falloff is not applied on the immediate path (no per-vertex gradient
        // available) - the tint is flat.
        noteShadowFallback(shadowFallbackReason(uvRect, tint));
        renderQuadsFallback(poseStack, packedLight, blockEntity, bufferSource, uvRect, tint);
    }

    /**
     * Shadow-pass fallback accounting: every part degrading to putBulkData in the
     * shadow pass defeats the global shadow batch (per-quad CPU cost through Iris's
     * extended vertex format), so the first reason per renderer is logged and every
     * occurrence is counted for the F3 shadow line. Main pass is not counted.
     */
    private void noteShadowFallback(String reason) {
        if (!com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        com.hbm_m.client.render.NucleusDebug.recordShadowFallback();
        if (shadowFallbackLogged) {
            return;
        }
        shadowFallbackLogged = true;
        com.hbm_m.main.MainRegistry.LOGGER.warn(
                "[HBM-M] Shadow fallback: part renderer '{}' degraded to putBulkData ({})",
                key, reason);
    }

    private String shadowFallbackReason(@Nullable float[] uvRect, @Nullable float[] tint) {
        if (instanced == null) return "instanced=null";
        if (!instanced.isInitialized()) return "instanced=uninitialized";
        if (!ClientRenderFlags.useInstancedBatching()) return "useInstancedBatching=false";
        if (uvRect != null) return "uvRect!=null";
        if (!isWhiteTint(tint)) return "tint!=white";
        return "single=null";
    }

    /** One-shot guard for the shadow-fallback reason warn log (the counter lives in NucleusDebug). */
    private boolean shadowFallbackLogged;

    /** null or white RGB - passthrough (the only case where the single-VBO path is allowed).
     *  Alpha is not checked: it carries emission strength (heat), not transparency. */
    private static boolean isWhiteTint(@Nullable float[] tint) {
        if (tint == null) return true;
        return tint.length >= 4
                && tint[0] == 1f && tint[1] == 1f && tint[2] == 1f;
    }

    /** Vanilla immediate: the universal last tier and the manual config reserve. */
    void renderQuadsFallback(PoseStack poseStack, int packedLight, BlockEntity blockEntity,
                             @Nullable MultiBufferSource bufferSource, @Nullable float[] uvRect,
                             @Nullable float[] tint) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        List<BakedQuad> drawn = quads;
        if (uvRect != null && uvRect.length >= 4
                && (uvRect[0] != 0f || uvRect[1] != 0f || uvRect[2] != 1f || uvRect[3] != 1f)) {
            // Expand normalized UVs into the skin's atlas rect (rare path).
            drawn = com.hbm_m.client.model.ModelHelper.expandQuadUvsUnit(quads,
                    uvRect[0], uvRect[1], uvRect[2], uvRect[3],
                    quads.get(0).getSprite());
        }
        com.hbm_m.client.render.NucleusDebug.recordDraw(1, 1, "Immediate (fallback)");
        float fade = SingleMeshVboRenderer.getFadeAlpha();
        float tr = 1f, tg = 1f, tb = 1f;
        if (tint != null && tint.length >= 4) {
            tr = tint[0]; tg = tint[1]; tb = tint[2];
        }
        float alpha = fade;
        // Cutout, not solid: solid has no alpha test, so transparent texels of glass/window
        // parts come out black. Cutout discards them; translucent stays for the fade path
        // (alpha blending).
        VertexConsumer consumer = bufferSource.getBuffer(alpha < 0.99f ? RenderType.translucent() : RenderType.cutout());
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : drawn) {
            // terrain RenderTypes do not compute shade from normals - bake it into
            // r/g/b, otherwise the immediate path draws machines flat (unlike the
            // VBO paths). The part tint multiplies on top (r/g/b may be > 1 -
            // overbright glow).
            float shade = RenderHooks.quadShade(quad.getDirection());
            RenderHooks.putBulkData(consumer, pose, quad,
                    shade * tr, shade * tg, shade * tb, alpha, packedLight,
                    OverlayTexture.NO_OVERLAY, false);
        }
    }

    void flush(Matrix4f projection) {
        if (instanced != null) {
            instanced.flush(projection);
        }
    }

    /**
     * Phase 2 (after MDI): fading instances of the direct path.
     * For MDI-compatible renderers - no-op (their fading was drawn by the coordinator).
     */
    void flushFading(Matrix4f projection) {
        if (instanced != null) {
            instanced.flushFading(projection);
        }
    }

    /** Key of the global fading-window sort (see InstancedStaticPartRenderer.fadingSortKeyDistSq). */
    float fadingSortKeyDistSq() {
        return (instanced != null) ? instanced.fadingSortKeyDistSq() : -1f;
    }

    void clear() {
        if (instanced != null) {
            instanced.cleanup();
            instanced = null;
        }
        single = null;   // owned by MeshRenderCache (it cleans it up)
        quads = null;
        attempted = false;
    }
}
