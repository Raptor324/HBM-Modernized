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
 * GPU-держатель ОДНОЙ части станка, создаваемый движком автоматически.
 * <p>
 * Цепочка деградации per draw (в порядке попыток):
 * <ol>
 *   <li>MDI (внутри {@link InstancedStaticPartRenderer#flush} — автоматически,
 *       если GL43/ARB доступны и нет shader pack);</li>
 *   <li>hardware instancing ({@code glDrawElementsInstanced});</li>
 *   <li>per-BE VBO ({@link SingleMeshVboRenderer});</li>
 *   <li>ванильный immediate ({@code putBulkData}) — также принудительно через
 *       {@code forceVanillaImmediatePath}.</li>
 * </ol>
 * Геометрия строится один раз лениво на render thread; до этого вызовы повторяются.
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

    /** true, если попытка построения уже была (успешной или нет) — квад resolver больше не вызывать. */
    boolean isAttempted() { return attempted; }

    boolean matches(MachineSpec.PartDef<?> part, String cacheKey) {
        return this.key.equals(cacheKey);
    }

    String key() { return key; }

    /** Ленивое построение. Вне render thread — просто отложить попытку. */
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
     * Добавляет текущий кадр-инстанс или рисует fallback-путём.
     *
     * @param poseStack  стек с блочным трансформом + аниматором части (composed pose)
     * @param sharedLight общий 8-corner световой сэмпл машины (или null)
     * @param uvRect {u0,v0,du,dv} ремапа sprite-local VBO → атлас или null (атласные UV).
     *               Не-null означает, что кэшированные quads нормализованы — путь
     *               single-VBO (без ремапа в шейдере) заменяется immediate-fallback.
     * @param tint {r,g,b,a} per-instance цветовой множитель или null (white). Не-white
     *               тинт на single-VBO пути тоже деградирует в immediate (шейдер
     *               block_lit тинта не читает); RGB может быть &gt; 1 — overbright.
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
        // uvRect != null: single-VBO путь держит нормализованные UV, а block_lit_simple
        // не умеет ремапить. Не-white tint: block_lit тинта не читает. Оба случая —
        // деградация в immediate (ремап UV в атлас + r/g/b/a в вершины). Фоллофф на
        // immediate-пути не применяется (пер-вершинный градиент недоступен) — тинт плоский.
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

    /** One-shot guard для warn-лога причины shadow-fallback (счётчик — в NucleusDebug). */
    private boolean shadowFallbackLogged;

    /** null или белый RGB — passthrough (единственный случай, когда single-VBO путь допустим).
     *  Альфу не проверяем: она несёт силу эмиссии (heat), а не прозрачность. */
    private static boolean isWhiteTint(@Nullable float[] tint) {
        if (tint == null) return true;
        return tint.length >= 4
                && tint[0] == 1f && tint[1] == 1f && tint[2] == 1f;
    }

    /** Ванильный immediate: универсальный последний уровень и ручной резерв из конфига. */
    void renderQuadsFallback(PoseStack poseStack, int packedLight, BlockEntity blockEntity,
                             @Nullable MultiBufferSource bufferSource, @Nullable float[] uvRect,
                             @Nullable float[] tint) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        List<BakedQuad> drawn = quads;
        if (uvRect != null && uvRect.length >= 4
                && (uvRect[0] != 0f || uvRect[1] != 0f || uvRect[2] != 1f || uvRect[3] != 1f)) {
            // Развёртывание нормализованных UV в атласный rect скина (редкий путь).
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
            // terrain RenderTypes shade из нормалей не считают — запекаем в r/g/b,
            // иначе immediate-путь рисует машины плоскими (в отличие от VBO-путей).
            // Тинт части домножается сверху (r/g/b могут быть > 1 — overbright-накал).
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
     * Фаза 2 (после MDI): затухающие инстансы прямого пути.
     * Для MDI-совместимых рендереров — no-op (их fading нарисовал координатор).
     */
    void flushFading(Matrix4f projection) {
        if (instanced != null) {
            instanced.flushFading(projection);
        }
    }

    /** Ключ глобальной сортировки fading-окон (см. InstancedStaticPartRenderer.fadingSortKeyDistSq). */
    float fadingSortKeyDistSq() {
        return (instanced != null) ? instanced.fadingSortKeyDistSq() : -1f;
    }

    void clear() {
        if (instanced != null) {
            instanced.cleanup();
            instanced = null;
        }
        single = null;   // владелец — MeshRenderCache (чистится им)
        quads = null;
        attempted = false;
    }
}
