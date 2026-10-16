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
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
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
                debugDoorBakedBbox(resolved);
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

    /**
     * Exec-гейт bbox-допущения дверных клип-плоскостей: фактические bbox запечённых
     * квадов по партам. Все дверные парты проходят через эту ветку как dynamicPart.
     * Включение: {@code -Dhbm.debugDoorBakedBbox=true}. Ожидания no-wrap — см.
     * javadoc DoorDecl.getClippingPlanes(partName, ...): airlock frame z[-1.5, 2.5],
     * doorLeft z[0.375, 2.438]; large_vehicle frame z[-3.5, 3.5]; sliding_blast
     * frame z[-3.5, 3.5]; fire door y[0, 3]. Пороги клип-плоскостей фиксировать
     * ТОЛЬКО после совпадения лога с ожиданиями.
     */
    private void debugDoorBakedBbox(List<BakedQuad> quads) {
        if (!Boolean.getBoolean("hbm.debugDoorBakedBbox")) return;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (BakedQuad q : quads) {
            int[] v = q.getVertices();
            int stride = v.length / 4;
            for (int i = 0; i < 4; i++) {
                float x = Float.intBitsToFloat(v[i * stride]);
                float y = Float.intBitsToFloat(v[i * stride + 1]);
                float z = Float.intBitsToFloat(v[i * stride + 2]);
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
            }
        }
        com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM-M] DoorBakedBbox part='{}': x[{}, {}] y[{}, {}] z[{}, {}] quads={}",
                partName, minX, maxX, minY, maxY, minZ, maxZ, quads.size());
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
     * @param clipPlanes per-part CPU-clip planes {{A,B,C,D}, ...} in the quad's LOCAL
     *               coordinates (convention "+D": inside = n·x + D &gt;= 0), or null.
     *               Non-null means the part draws through the clipped immediate tier
     *               (Sutherland-Hodgman over the cached quads, meshes are NOT rebuilt -
     *               parity of ntm-next GroupObject.emitClipped); forceVanillaImmediate/
     *               diagram goes through the same tier.
     */
    void enqueue(PoseStack poseStack,
                 int packedLight, BlockPos blockPos, BlockEntity blockEntity,
                 @Nullable MultiBufferSource bufferSource, @Nullable float[] sharedLight,
                 @Nullable float[] uvRect, @Nullable float[] tint, @Nullable float[] gradFalloff,
                 @Nullable float[][] clipPlanes) {
        // Clipped tier (door pockets): per-frame CPU clip of the cached quads, immediate
        // draw. Checked BEFORE the deferred paths - the clipped result is per-frame.
        if (clipPlanes != null) {
            noteShadowFallback(shadowFallbackReason(clipPlanes, uvRect, tint));
            renderClippedQuads(poseStack, packedLight, bufferSource, uvRect, tint, clipPlanes);
            return;
        }
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
        noteShadowFallback(shadowFallbackReason(null, uvRect, tint));
        renderQuadsFallback(poseStack, packedLight, blockEntity, bufferSource, uvRect, tint);
    }

    /**
     * Clipped immediate tier (door pockets): Sutherland-Hodgman over the CACHED
     * this.quads (meshes are not rebuilt), result emitted into cutout/translucent
     * (shared {@code ModelHelper.clipRenderType}). Same shading/tint/fade semantics
     * as {@link #renderQuadsFallback}; uvRect is passed to the clipper AS IS -- it is
     * itself the NORMALIZED marker (uvRect != null -- quads are sprite-local, the
     * clipper remaps them by the same rect as InstUvRect/expandQuadUvsUnit, memoized;
     * null -- atlas quads, pure geometric clip). No separate space tag exists
     * (canonical design rev.2: one source of truth).
     */
    private void renderClippedQuads(PoseStack poseStack, int packedLight,
                                    @Nullable MultiBufferSource bufferSource,
                                    @Nullable float[] uvRect, @Nullable float[] tint,
                                    float[][] clipPlanes) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        // Дебаг-дамп клип-тира (конфиг doorClipDebug): фактические спрайт/UV входных квадов.
        if (com.hbm_m.config.ModClothConfig.get().doorClipDebug) {
            debugDumpQuads();
        }
        List<BakedQuad> clipped = DoorPartClipper.clipQuads(quads, clipPlanes, uvRect);
        if (clipped.isEmpty()) return;
        com.hbm_m.client.render.NucleusDebug.recordDraw(1, 1, "Immediate (clipped)");
        float fade = SingleMeshVboRenderer.getFadeAlpha();
        float tr = 1f, tg = 1f, tb = 1f;
        if (tint != null && tint.length >= 4) {
            tr = tint[0]; tg = tint[1]; tb = tint[2];
        }
        float alpha = fade;

        // Cutout/translucent - same rationale as renderQuadsFallback (shared helper).
        VertexConsumer consumer = bufferSource.getBuffer(
                com.hbm_m.client.model.ModelHelper.clipRenderType(alpha));
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : clipped) {
            // terrain RenderTypes do not compute shade from normals - bake it into r/g/b.
            float shade = RenderHooks.quadShade(quad.getDirection());
            RenderHooks.putBulkData(consumer, pose, quad,
                    shade * tr, shade * tg, shade * tb, alpha, packedLight,
                    OverlayTexture.NO_OVERLAY, false);
        }
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

    private String shadowFallbackReason(@Nullable float[][] clipPlanes, @Nullable float[] uvRect,
                                        @Nullable float[] tint) {
        if (clipPlanes != null) return "clipPlanes!=null";
        if (instanced == null) return "instanced=null";
        if (!instanced.isInitialized()) return "instanced=uninitialized";
        if (!ClientRenderFlags.useInstancedBatching()) return "useInstancedBatching=false";
        if (uvRect != null) return "uvRect!=null";
        if (!isWhiteTint(tint)) return "tint!=white";
        return "single=null";
    }

    /** One-shot guard for the shadow-fallback reason warn log (the counter lives in NucleusDebug). */
    private boolean shadowFallbackLogged;

    // ==== Дебаг клип-тира (конфиг doorClipDebug): одноразовый дамп спрайтов/UV партов ====
    private static final java.util.Set<String> CLIP_DEBUG_DUMPED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void debugDumpQuads() {
        if (!CLIP_DEBUG_DUMPED.add(key)) return;
        StringBuilder sb = new StringBuilder("part='").append(key).append("' quads=").append(quads.size());
        int n = Math.min(quads.size(), 6);
        for (int i = 0; i < n; i++) {
            BakedQuad q = quads.get(i);
            int[] v = q.getVertices();
            int stride = v.length / 4;
            float uMin = Float.MAX_VALUE, uMax = -Float.MAX_VALUE, vMin = Float.MAX_VALUE, vMax = -Float.MAX_VALUE;
            float xMin = Float.MAX_VALUE, xMax = -Float.MAX_VALUE, yMin = Float.MAX_VALUE, yMax = -Float.MAX_VALUE, zMin = Float.MAX_VALUE, zMax = -Float.MAX_VALUE;
            for (int vi = 0; vi < 4; vi++) {
                float u = Float.intBitsToFloat(v[vi * stride + 4]);
                float vv = Float.intBitsToFloat(v[vi * stride + 5]);
                uMin = Math.min(uMin, u); uMax = Math.max(uMax, u);
                vMin = Math.min(vMin, vv); vMax = Math.max(vMax, vv);
                float px = Float.intBitsToFloat(v[vi * stride]);
                float py = Float.intBitsToFloat(v[vi * stride + 1]);
                float pz = Float.intBitsToFloat(v[vi * stride + 2]);
                xMin = Math.min(xMin, px); xMax = Math.max(xMax, px);
                yMin = Math.min(yMin, py); yMax = Math.max(yMax, py);
                zMin = Math.min(zMin, pz); zMax = Math.max(zMax, pz);
            }
            var s = q.getSprite();
            sb.append(String.format("%n  quad[%d] sprite=%s atlasRect=[%.1f..%.1f, %.1f..%.1f] pos=[%.3f..%.3f, %.3f..%.3f, %.3f..%.3f] uv=[%.4f..%.4f, %.4f..%.4f] stride=%d",
                    i, s == null ? "null" : s.contents().name(),
                    s == null ? -1 : s.getU0(), s == null ? -1 : s.getU1(),
                    s == null ? -1 : s.getV0(), s == null ? -1 : s.getV1(),
                    uMin, uMax, vMin, vMax,
                    xMin, xMax, yMin, yMax, zMin, zMax, stride));
        }
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM-M][DoorClipDebug] {}", sb);
    }

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
        if (!com.hbm_m.client.model.ModelHelper.isIdentityRect(uvRect)) {
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
        // (alpha blending). Shared helper (dedup with renderClippedQuads).
        VertexConsumer consumer = bufferSource.getBuffer(
                com.hbm_m.client.model.ModelHelper.clipRenderType(alpha));
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
